package com.nanami.koishi.feature.tools.lan_transfer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.lan_transfer.engine.discovery.DeviceDiscovery
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalDeviceInfoProvider
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityStore
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingFileResult
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingFileUpload
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingUploadDecision
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingUploadRequest
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServer
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServerCallbacks
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServerConfig
import com.nanami.koishi.feature.tools.lan_transfer.engine.settings.LanTransferSettingsRepository
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ContentUriFileSource
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.LanTransferFileStore
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.PickedFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class LanTransferViewModel(
    application: Application,
    private val settingsRepository: LanTransferSettingsRepository,
    identityStore: LocalSendIdentityStore,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val client: LocalSendClient,
    private val discovery: DeviceDiscovery
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LanTransferUiState())
    val uiState: StateFlow<LanTransferUiState> = _uiState.asStateFlow()

    private val fileStore = LanTransferFileStore(application)
    private val pendingOutputs = ConcurrentHashMap<String, LanTransferFileStore.PendingFile>()
    private val devicesById = ConcurrentHashMap<String, LanDevice>()
    private var pendingDecision: CompletableDeferred<IncomingUploadDecision>? = null
    private var server: LocalSendServer? = null
    private val localFingerprint = identityStore.get().fingerprint

    private val TRANSPORT_PROTOCOL = LanDeviceProtocol.HTTP

    init {
        viewModelScope.launch {
            val settings = settingsRepository.currentData()
            _uiState.update {
                it.copy(
                    alias = settings.alias.ifBlank { defaultAlias() },
                    receiveEnabled = settings.receiveEnabled,
                    isLoading = false
                )
            }
            if (settings.receiveEnabled) startServer()
            restartDiscovery()
        }
    }

    fun onEvent(event: LanTransferUiEvent) {
        when (event) {
            is LanTransferUiEvent.OnToggleReceive -> setReceiveEnabled(event.enabled)
            LanTransferUiEvent.OnRefresh -> refreshDiscovery()
            is LanTransferUiEvent.OnFilesPicked -> {
                _uiState.update { it.copy(selectedFiles = event.files) }
                if (event.files.isNotEmpty()) {
                    message(R.string.lan_transfer_files_selected, event.files.size.toString())
                }
            }
            is LanTransferUiEvent.OnSendTo -> sendTo(event.deviceId)
            is LanTransferUiEvent.OnAcceptIncoming -> resolveIncoming(event.accepted)
            LanTransferUiEvent.OnDismissMessage -> _uiState.update {
                it.copy(userMessageRes = null, userMessageArgs = emptyList())
            }
            is LanTransferUiEvent.OnDismissTransfer -> _uiState.update { state ->
                state.copy(transfers = state.transfers.filterNot { it.id == event.id })
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryJob?.cancel()
        runBlocking { server?.stop() }
        server = null
        LanTransferForegroundService.stop(getApplication())
    }

    private fun localAlias(): String = _uiState.value.alias

    private fun defaultAlias(): String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

    private var discoveryJob: Job? = null

    private fun restartDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            discovery.discover(
                alias = localAlias(),
                protocol = TRANSPORT_PROTOCOL,
                announce = _uiState.value.receiveEnabled
            ).collect { device ->
                if (device.fingerprint == localFingerprint) return@collect
                devicesById[device.id] = device
                _uiState.update { it.copy(devices = devicesById.values.sortedBy { device -> device.alias }) }
            }
        }
    }

    private fun refreshDiscovery() {
        devicesById.clear()
        _uiState.update { it.copy(devices = emptyList()) }
        restartDiscovery()
    }

    private fun setReceiveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateData { it.copy(receiveEnabled = enabled) }
            _uiState.update { it.copy(receiveEnabled = enabled) }
            if (enabled) {
                LanTransferForegroundService.start(getApplication())
                startServer()
            } else {
                stopServer()
                LanTransferForegroundService.stop(getApplication())
            }
            restartDiscovery()
            message(if (enabled) R.string.lan_transfer_receive_on else R.string.lan_transfer_receive_off)
        }
    }

    private suspend fun startServer() {
        if (server != null) return
        val settings = settingsRepository.currentData()
        val deviceInfo = deviceInfoProvider.create(localAlias(), protocol = TRANSPORT_PROTOCOL)
        val config = LocalSendServerConfig(
            device = DeviceInfoResponseDto(
                alias = deviceInfo.alias,
                version = deviceInfo.version,
                deviceModel = deviceInfo.deviceModel,
                deviceType = deviceInfo.deviceType,
                fingerprint = deviceInfo.fingerprint,
                download = false
            ),
            pin = settings.pin.takeIf { settings.pinEnabled && it.isNotBlank() },
            verifyChecksums = false
        )
        val instance = LocalSendServer(config, ServerCallbacks())
        instance.start(port = LocalSendProtocol.DEFAULT_PORT)
        server = instance
    }

    private suspend fun stopServer() {
        server?.stop()
        server = null
        pendingDecision?.cancel()
        pendingDecision = null
        _uiState.update { it.copy(incomingRequest = null) }
    }

    private fun sendTo(deviceId: String) {
        val device = devicesById[deviceId] ?: return
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) {
            message(R.string.lan_transfer_no_files)
            return
        }
        viewModelScope.launch {
            val entries = files.mapIndexed { index, picked ->
                TransferFileDto(
                    id = "file_${index}_${picked.fileName.hashCode()}",
                    fileName = picked.fileName,
                    size = picked.size,
                    fileType = picked.mimeType,
                    sha256 = null,
                    preview = null,
                    metadata = null
                )
            }
            val byId = entries.associateBy { it.id }
            addTransfers(entries, TransferDirection.SENDING)
            try {
                val settings = settingsRepository.currentData()
                val localInfo = deviceInfoProvider.create(localAlias(), protocol = TRANSPORT_PROTOCOL)
                val response = client.prepareUpload(
                    host = device.host,
                    port = device.port,
                    protocol = device.protocol,
                    payload = PrepareUploadRequestDto(info = localInfo, files = byId),
                    pin = settings.pin.takeIf { settings.pinEnabled && it.isNotBlank() }
                )
                if (response == null) {
                    markAllFailed(entries, TransferDirection.SENDING)
                    return@launch
                }
                entries.forEach { dto ->
                    val token = response.files[dto.id] ?: return@forEach
                    val picked = files.first { it.fileName == dto.fileName }
                    client.upload(
                        host = device.host,
                        port = device.port,
                        protocol = device.protocol,
                        sessionId = response.sessionId,
                        fileId = dto.id,
                        token = token,
                        source = ContentUriFileSource.source(getApplication<Application>().contentResolver, picked),
                        onProgress = { sent -> updateProgress(dto.id, sent, dto.size) }
                    )
                    updateStatus(dto.id, TransferItemStatus.DONE)
                }
                _uiState.update { it.copy(selectedFiles = emptyList()) }
            } catch (error: Exception) {
                markAllFailed(entries, TransferDirection.SENDING)
                message(R.string.lan_transfer_error_generic)
            }
        }
    }

    private fun resolveIncoming(accepted: Boolean) {
        val decision = pendingDecision ?: return
        val request = _uiState.value.incomingRequest
        if (accepted && request != null) {
            val ids = _uiState.value.transfers
                .filter { it.direction == TransferDirection.RECEIVING }
                .map { it.id }
                .toSet()
            decision.complete(IncomingUploadDecision.Accept(ids))
        } else {
            decision.complete(IncomingUploadDecision.Reject)
        }
        pendingDecision = null
        _uiState.update { it.copy(incomingRequest = null) }
    }

    private fun addTransfers(files: List<TransferFileDto>, direction: TransferDirection) {
        val items = files.map { dto ->
            TransferItemUi(
                id = dto.id,
                fileName = dto.fileName,
                direction = direction,
                progress = 0f,
                status = TransferItemStatus.IN_PROGRESS
            )
        }
        _uiState.update { it.copy(transfers = items + it.transfers) }
    }

    private fun updateProgress(fileId: String, sent: Long, total: Long) {
        val progress = if (total <= 0) 0f else (sent.toFloat() / total).coerceIn(0f, 1f)
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.id == fileId) item.copy(progress = progress) else item
            })
        }
    }

    private fun updateStatus(fileId: String, status: TransferItemStatus) {
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.id == fileId) item.copy(status = status, progress = if (status == TransferItemStatus.DONE) 1f else item.progress) else item
            })
        }
    }

    private fun markAllFailed(files: List<TransferFileDto>, direction: TransferDirection) {
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.direction == direction && files.any { it.id == item.id }) {
                    item.copy(status = TransferItemStatus.FAILED)
                } else {
                    item
                }
            })
        }
    }

    private fun message(resId: Int, vararg args: String) {
        _uiState.update { it.copy(userMessageRes = resId, userMessageArgs = args.toList()) }
    }

    private inner class ServerCallbacks : LocalSendServerCallbacks {

        override suspend fun onUploadRequested(request: IncomingUploadRequest): IncomingUploadDecision {
            val deferred = CompletableDeferred<IncomingUploadDecision>()
            pendingDecision = deferred
            addTransfers(request.files.values.toList(), TransferDirection.RECEIVING)
            _uiState.update {
                it.copy(
                    incomingRequest = IncomingRequestUi(
                        sessionId = request.sessionId,
                        peerAlias = request.peer.alias,
                        fileCount = request.files.size,
                        totalBytes = request.totalBytes
                    )
                )
            }
            return deferred.await()
        }

        override suspend fun openFileTarget(request: IncomingFileUpload): OutputStream {
            val pending = fileStore.open(request.file)
            pendingOutputs["${request.sessionId}/${request.file.id}"] = pending
            return pending.output
        }

        override suspend fun onFileFinished(result: IncomingFileResult) {
            val key = "${result.sessionId}/${result.file.id}"
            pendingOutputs.remove(key)?.publish(result.success)
            updateStatus(result.file.id, if (result.success) TransferItemStatus.DONE else TransferItemStatus.FAILED)
        }
    }
}
