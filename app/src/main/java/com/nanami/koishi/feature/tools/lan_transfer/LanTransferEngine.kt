package com.nanami.koishi.feature.tools.lan_transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.lan_transfer.engine.discovery.DeviceDiscovery
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.DeviceInfoResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.FileMetadataDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalDeviceInfoProvider
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocol
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendProtocolException
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadRequestDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.PrepareUploadResponseDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.TransferFileDto
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityStore
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingFileResult
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingFileUpload
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingUploadDecision
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.IncomingUploadRequest
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServer
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServerCallbacks
import com.nanami.koishi.feature.tools.lan_transfer.engine.server.LocalSendServerConfig
import com.nanami.koishi.feature.tools.lan_transfer.engine.settings.LanTransferSettings
import com.nanami.koishi.feature.tools.lan_transfer.engine.settings.LanTransferSettingsRepository
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ContentUriFileSource
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.LanTransferFileStore
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.PickedFile
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ReceivedFileLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class LanTransferEngine(
    context: Context,
    private val settingsRepository: LanTransferSettingsRepository,
    identityStore: LocalSendIdentityStore,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val client: LocalSendClient,
    private val discovery: DeviceDiscovery
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val initialized = AtomicBoolean(false)

    private val _uiState = MutableStateFlow(LanTransferUiState())
    val uiState: StateFlow<LanTransferUiState> = _uiState.asStateFlow()

    private val fileStore = LanTransferFileStore(appContext)
    private val pendingOutputs = ConcurrentHashMap<String, LanTransferFileStore.PendingFile>()
    private val devicesById = ConcurrentHashMap<String, LanDevice>()
    private val retrySends = ConcurrentHashMap<String, RetrySend>()
    private var pendingDecision: CompletableDeferred<IncomingUploadDecision>? = null
    private var pendingPinDecision: CompletableDeferred<String?>? = null
    private var server: LocalSendServer? = null
    private var discoveryJob: Job? = null
    private var sendJob: Job? = null
    private var activeSendSession: ActiveSendSession? = null
    private val identity = identityStore.get()
    private val localFingerprint = identity.fingerprint

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch {
            val settings = settingsRepository.currentData()
            _uiState.update { state ->
                state.copy(
                    alias = settings.alias.ifBlank { defaultAlias() },
                    receiveEnabled = settings.receiveEnabled,
                    settings = settings.toUi(),
                    isLoading = false
                )
            }
            startServer()
            restartDiscovery()
        }
    }

    fun addSharedFiles(files: List<PickedFile>) {
        if (files.isNotEmpty()) onEvent(LanTransferUiEvent.OnFilesPicked(files))
    }

    fun onEvent(event: LanTransferUiEvent) {
        when (event) {
            is LanTransferUiEvent.OnToggleReceive -> setReceiveEnabled(event.enabled)
            LanTransferUiEvent.OnRefresh -> refreshDiscovery()
            is LanTransferUiEvent.OnFilesPicked -> {
                val selected = _uiState.value.selectedFiles
                    .plus(event.files)
                    .distinctBy { it.uri.toString() }
                _uiState.update { it.copy(selectedFiles = selected) }
                if (event.files.isNotEmpty()) {
                    message(R.string.lan_transfer_files_selected, selected.size.toString())
                }
            }
            is LanTransferUiEvent.OnRemoveFile -> _uiState.update { state ->
                state.copy(selectedFiles = state.selectedFiles.filterNot { it.uri.toString() == event.uri })
            }
            LanTransferUiEvent.OnClearFiles -> _uiState.update { it.copy(selectedFiles = emptyList()) }
            is LanTransferUiEvent.OnSendTo -> sendTo(event.deviceId)
            is LanTransferUiEvent.OnSendText -> sendText(event.deviceId, event.text)
            is LanTransferUiEvent.OnAcceptIncoming -> resolveIncoming(event.accepted)
            is LanTransferUiEvent.OnSubmitPin -> pendingPinDecision?.complete(event.pin.trim())
            LanTransferUiEvent.OnCancelPinPrompt -> pendingPinDecision?.complete(null)
            is LanTransferUiEvent.OnCancelTransfer -> cancelTransfer(event.id)
            is LanTransferUiEvent.OnRetryTransfer -> retryTransfer(event.id)
            is LanTransferUiEvent.OnSaveSettings -> saveSettings(event)
            LanTransferUiEvent.OnDismissMessage -> _uiState.update {
                it.copy(userMessageRes = null, userMessageArgs = emptyList())
            }
            is LanTransferUiEvent.OnDismissTransfer -> {
                cleanupTemporary(event.id)
                retrySends.remove(event.id)
                _uiState.update { state ->
                    state.copy(transfers = state.transfers.filterNot { it.id == event.id })
                }
            }
        }
    }


    private fun localAlias(): String = _uiState.value.alias

    private fun defaultAlias(): String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

    private fun restartDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val scanningJob = launch {
                delay(SCAN_INDICATOR_DURATION_MS)
                _uiState.update { it.copy(isScanning = false) }
            }
            try {
                discovery.discover(
                    alias = localAlias(),
                    serverPort = server?.port ?: LocalSendProtocol.DEFAULT_PORT,
                    protocol = LanDeviceProtocol.HTTPS,
                    announce = true
                ).collect { device ->
                    if (device.fingerprint == localFingerprint) return@collect
                    devicesById[device.id] = device
                    _uiState.update { it.copy(devices = devicesById.values.sortedBy { device -> device.alias }) }
                }
            } finally {
                scanningJob.cancel()
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun refreshDiscovery() {
        devicesById.clear()
        _uiState.update { it.copy(devices = emptyList()) }
        restartDiscovery()
    }

    private fun setReceiveEnabled(enabled: Boolean) {
        scope.launch {
            val settings = settingsRepository.updateData { it.copy(receiveEnabled = enabled) }
            _uiState.update { it.copy(receiveEnabled = enabled, settings = settings.toUi()) }
            if (enabled) {
                LanTransferForegroundService.start(appContext)
            } else {
                LanTransferForegroundService.stop(appContext)
            }
            restartDiscovery()
            message(if (enabled) R.string.lan_transfer_receive_on else R.string.lan_transfer_receive_off)
        }
    }

    private suspend fun startServer() {
        if (server != null) return
        val settings = settingsRepository.currentData()
        val deviceInfo = deviceInfoProvider.create(localAlias(), protocol = LanDeviceProtocol.HTTPS)
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
            verifyChecksums = settings.verifyChecksums
        )
        val instance = LocalSendServer(config, identity, ServerCallbacks())
        try {
            instance.start(port = LocalSendProtocol.DEFAULT_PORT)
            server = instance
        } catch (_: Exception) {
            val fallback = LocalSendServer(config, identity, ServerCallbacks())
            fallback.start(port = 0)
            server = fallback
        }
    }

    private suspend fun stopServer() {
        server?.stop()
        server = null
        pendingDecision?.cancel()
        pendingDecision = null
        _uiState.update { it.copy(incomingRequest = null) }
    }

    private fun saveSettings(event: LanTransferUiEvent.OnSaveSettings) {
        scope.launch {
            val settings = settingsRepository.updateData {
                it.copy(
                    alias = event.alias.trim(),
                    pinEnabled = event.pinEnabled,
                    pin = event.pin.trim(),
                    saveDirectory = event.saveDirectory.trim().ifBlank { "Koishi" },
                    autoAcceptKnownDevices = event.autoAccept,
                    computeChecksums = event.computeChecksums,
                    verifyChecksums = event.verifyChecksums
                )
            }
            _uiState.update {
                it.copy(
                    alias = settings.alias.ifBlank { defaultAlias() },
                    settings = settings.toUi()
                )
            }
            if (server != null) {
                stopServer()
                startServer()
            }
            restartDiscovery()
            message(R.string.lan_transfer_settings_saved)
        }
    }

    private fun sendTo(deviceId: String) {
        val device = devicesById[deviceId] ?: return
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) {
            message(R.string.lan_transfer_no_files)
            return
        }
        startSend(device, files, clearSelection = true)
    }

    private fun sendText(deviceId: String, text: String) {
        val device = devicesById[deviceId] ?: return
        if (text.isBlank()) return
        val content = text
        val bytes = content.toByteArray(Charsets.UTF_8)
        val directory = File(appContext.cacheDir, "lan_transfer_text").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.txt")
        file.writeBytes(bytes)
        val picked = PickedFile(
            uri = Uri.fromFile(file),
            fileName = file.name,
            size = bytes.size.toLong(),
            mimeType = "text/plain",
            previewText = content,
            temporary = true,
            displayName = appContext.getString(R.string.lan_transfer_text_message)
        )
        startSend(device, listOf(picked), clearSelection = false)
    }

    private fun startSend(device: LanDevice, files: List<PickedFile>, clearSelection: Boolean) {
        if (sendJob?.isActive == true) {
            message(R.string.lan_transfer_busy)
            return
        }
        sendJob = scope.launch {
            _uiState.update { it.copy(activeSendDeviceId = device.id) }
            try {
                val entries = prepareSendEntries(files)
                addTransfers(
                    files = entries.map { it.dto },
                    direction = TransferDirection.SENDING,
                    displayNames = entries.associate { it.dto.id to (it.picked.displayName ?: it.dto.fileName) }
                )
                entries.forEach { entry -> retrySends[entry.dto.id] = RetrySend(device, entry) }
                processSend(device, entries)
                if (clearSelection) _uiState.update { it.copy(selectedFiles = emptyList()) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                message(R.string.lan_transfer_error_generic)
            } finally {
                activeSendSession = null
                sendJob = null
                _uiState.update { it.copy(activeSendDeviceId = null) }
            }
        }
    }

    private suspend fun prepareSendEntries(files: List<PickedFile>): List<PreparedSendEntry> {
        val computeChecksums = settingsRepository.currentData().computeChecksums
        return files.map { picked ->
            val metadata = picked.toMetadata()
            TransferFileDto(
                id = "file_${UUID.randomUUID()}",
                fileName = picked.fileName,
                size = picked.size,
                fileType = picked.mimeType,
                sha256 = if (computeChecksums) {
                    try {
                        ContentUriFileSource.sha256(appContext.contentResolver, picked)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                },
                preview = picked.previewText,
                metadata = metadata
            ).let { dto -> PreparedSendEntry(dto, picked) }
        }
    }

    private suspend fun processSend(device: LanDevice, entries: List<PreparedSendEntry>) {
        if (entries.isEmpty()) return
        val localInfo = deviceInfoProvider.create(localAlias(), protocol = LanDeviceProtocol.HTTPS)
        var response: PrepareUploadResponseDto? = null
        var pin: String? = null
        var pinAttempt = 0
        while (true) {
            try {
                Log.d(TAG, "prepare-upload to ${device.host}:${device.port} files=${entries.size}")
                response = client.prepareUpload(
                    host = device.host,
                    port = device.port,
                    protocol = device.protocol,
                    payload = PrepareUploadRequestDto(
                        info = localInfo,
                        files = entries.associate { it.dto.id to it.dto }
                    ),
                    pin = pin,
                    expectedFingerprint = device.fingerprint
                )
                break
            } catch (error: CancellationException) {
                throw error
            } catch (error: LocalSendProtocolException) {
                if (error.statusCode == 401) {
                    val enteredPin = requestPin(
                        device = device,
                        invalid = pinAttempt > 0,
                        attempt = pinAttempt
                    ) ?: run {
                        markEntriesCancelled(entries.map { it.dto })
                        return
                    }
                    pin = enteredPin
                    pinAttempt += 1
                    continue
                }
                Log.e(TAG, "prepare-upload failed", error)
                markEntriesFailed(entries.map { it.dto }, error.message)
                return
            } catch (error: Exception) {
                Log.e(TAG, "prepare-upload failed", error)
                markEntriesFailed(entries.map { it.dto }, error.message)
                return
            }
        }
        if (response == null) {
            entries.forEach { entry ->
                updateStatus(entry.dto.id, TransferItemStatus.DONE)
                cleanupTemporary(entry.dto.id)
                retrySends.remove(entry.dto.id)
            }
            return
        }

        activeSendSession = ActiveSendSession(response.sessionId, device)
        updateSessionId(entries.map { it.dto.id }, response.sessionId)
        entries.forEach { entry ->
            val token = response.files[entry.dto.id]
            if (token == null) {
                updateStatus(entry.dto.id, TransferItemStatus.FAILED, canRetry = true)
                return@forEach
            }
            var uploaded = false
            var lastError: String? = null
            repeat(MAX_UPLOAD_ATTEMPTS) { attempt ->
                try {
                    client.upload(
                        host = device.host,
                        port = device.port,
                        protocol = device.protocol,
                        sessionId = response.sessionId,
                        fileId = entry.dto.id,
                        token = token,
                        source = ContentUriFileSource.source(
                            appContext.contentResolver,
                            entry.picked
                        ),
                        expectedFingerprint = device.fingerprint,
                        onProgress = { sent -> updateProgress(entry.dto.id, sent, entry.dto.size) }
                    )
                    uploaded = true
                    return@repeat
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    lastError = error.message ?: error.javaClass.simpleName
                    Log.e(TAG, "upload failed for ${entry.dto.fileName}", error)
                    if (attempt < MAX_UPLOAD_ATTEMPTS - 1) {
                        delay(RETRY_DELAY_MS * (attempt + 1))
                    }
                }
            }
            if (uploaded) {
                updateStatus(entry.dto.id, TransferItemStatus.DONE)
                cleanupTemporary(entry.dto.id)
                retrySends.remove(entry.dto.id)
            } else {
                updateStatus(
                    entry.dto.id,
                    TransferItemStatus.FAILED,
                    canRetry = true,
                    errorMessage = lastError
                )
            }
        }
        activeSendSession = null
    }

    fun cancelPendingPinPrompt() {
        pendingPinDecision?.complete(null)
        pendingPinDecision = null
        _uiState.update { it.copy(pinPrompt = null) }
    }

    private suspend fun requestPin(
        device: LanDevice,
        invalid: Boolean,
        attempt: Int
    ): String? {
        pendingPinDecision?.complete(null)
        val deferred = CompletableDeferred<String?>()
        pendingPinDecision = deferred
        _uiState.update {
            it.copy(
                pinPrompt = PinPromptUi(
                    deviceId = device.id,
                    deviceAlias = device.alias,
                    invalid = invalid,
                    attempt = attempt
                )
            )
        }
        return try {
            deferred.await()
        } finally {
            if (pendingPinDecision === deferred) {
                pendingPinDecision = null
            }
            _uiState.update { it.copy(pinPrompt = null) }
        }
    }

    private fun retryTransfer(id: String) {
        val retry = retrySends[id] ?: return
        if (sendJob?.isActive == true) {
            message(R.string.lan_transfer_busy)
            return
        }
        updateStatus(id, TransferItemStatus.IN_PROGRESS, canRetry = false)
        sendJob = scope.launch {
            _uiState.update { it.copy(activeSendDeviceId = retry.device.id) }
            try {
                processSend(retry.device, listOf(retry.entry))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateStatus(id, TransferItemStatus.FAILED, canRetry = true)
                message(R.string.lan_transfer_error_generic)
            } finally {
                activeSendSession = null
                sendJob = null
                _uiState.update { it.copy(activeSendDeviceId = null) }
            }
        }
    }

    private fun cancelTransfer(id: String) {
        val item = _uiState.value.transfers.firstOrNull { it.id == id } ?: return
        when (item.direction) {
            TransferDirection.SENDING -> cancelSendingTransfer()
            TransferDirection.RECEIVING -> cancelReceivingTransfer(item.sessionId)
        }
    }

    private fun cancelSendingTransfer() {
        val session = activeSendSession
        cancelPendingPinPrompt()
        sendJob?.cancel()
        sendJob = null
        activeSendSession = null
        markInProgressCancelled(TransferDirection.SENDING, sessionId = null)
        if (session != null) {
            scope.launch {
                runCatching {
                    client.cancel(
                        host = session.device.host,
                        port = session.device.port,
                        protocol = session.device.protocol,
                        sessionId = session.sessionId,
                        expectedFingerprint = session.device.fingerprint
                    )
                }
            }
        }
    }

    private fun cancelReceivingTransfer(sessionId: String?) {
        if (sessionId == null) return
        scope.launch {
            val peer = server?.cancelSession(sessionId)
            markInProgressCancelled(TransferDirection.RECEIVING, sessionId)
            if (peer != null) {
                runCatching {
                    client.cancel(
                        host = peer.host,
                        port = peer.port,
                        protocol = peer.protocol,
                        sessionId = sessionId,
                        expectedFingerprint = peer.fingerprint
                    )
                }
            }
        }
    }

    private fun resolveIncoming(accepted: Boolean) {
        val decision = pendingDecision ?: return
        val request = _uiState.value.incomingRequest
        if (accepted && request != null) {
            val ids = when (request) {
                is IncomingRequestUi.Files -> _uiState.value.transfers
                    .filter { it.direction == TransferDirection.RECEIVING && it.sessionId == request.sessionId }
                    .map { it.id }
                    .toSet()

                is IncomingRequestUi.Text -> emptySet()
            }
            decision.complete(IncomingUploadDecision.Accept(ids))
        } else {
            decision.complete(IncomingUploadDecision.Reject)
            request?.sessionId?.let { markInProgressCancelled(TransferDirection.RECEIVING, it) }
        }
        pendingDecision = null
        _uiState.update { it.copy(incomingRequest = null) }
    }

    private fun addTransfers(
        files: List<TransferFileDto>,
        direction: TransferDirection,
        sessionId: String? = null,
        displayNames: Map<String, String> = emptyMap()
    ) {
        val items = files.map { dto ->
            TransferItemUi(
                id = dto.id,
                fileName = displayNames[dto.id] ?: dto.fileName,
                direction = direction,
                progress = 0f,
                status = TransferItemStatus.IN_PROGRESS,
                sessionId = sessionId
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

    private fun updateStatus(
        fileId: String,
        status: TransferItemStatus,
        canRetry: Boolean = false,
        errorMessage: String? = null
    ) {
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.id == fileId && item.status != TransferItemStatus.CANCELLED) {
                    item.copy(
                        status = status,
                        progress = if (status == TransferItemStatus.DONE) 1f else item.progress,
                        canRetry = canRetry && status == TransferItemStatus.FAILED,
                        errorMessage = errorMessage
                    )
                } else {
                    item
                }
            })
        }
    }

    private fun updateReceivedLocation(fileId: String, location: ReceivedFileLocation) {
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.id == fileId) item.copy(receivedLocation = location) else item
            })
        }
    }

    private fun updateSessionId(fileIds: List<String>, sessionId: String) {
        val ids = fileIds.toSet()
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.id in ids) item.copy(sessionId = sessionId) else item
            })
        }
    }

    private fun markEntriesFailed(files: List<TransferFileDto>, errorMessage: String? = null) {
        files.forEach {
            updateStatus(it.id, TransferItemStatus.FAILED, canRetry = true, errorMessage = errorMessage)
        }
    }

    private fun markEntriesCancelled(files: List<TransferFileDto>) {
        files.forEach {
            updateStatus(it.id, TransferItemStatus.CANCELLED)
        }
    }

    private fun markInProgressCancelled(direction: TransferDirection, sessionId: String?) {
        _uiState.update { state ->
            state.copy(transfers = state.transfers.map { item ->
                if (item.direction == direction &&
                    item.status == TransferItemStatus.IN_PROGRESS &&
                    (sessionId == null || item.sessionId == sessionId)
                ) {
                    item.copy(status = TransferItemStatus.CANCELLED, canRetry = false)
                } else {
                    item
                }
            })
        }
    }

    private fun cleanupTemporary(fileId: String) {
        val picked = retrySends[fileId]?.entry?.picked ?: return
        if (!picked.temporary) return
        picked.uri.path?.let { path -> runCatching { File(path).delete() } }
    }

    private fun message(resId: Int, vararg args: String) {
        _uiState.update { it.copy(userMessageRes = resId, userMessageArgs = args.toList()) }
    }

    private inner class ServerCallbacks : LocalSendServerCallbacks {

        override suspend fun onUploadRequested(request: IncomingUploadRequest): IncomingUploadDecision {
            if (!_uiState.value.receiveEnabled) return IncomingUploadDecision.Reject
            val message = request.files.singleTextMessageOrNull()
            if (message != null) {
                val deferred = CompletableDeferred<IncomingUploadDecision>()
                pendingDecision = deferred
                _uiState.update {
                    it.copy(
                        incomingRequest = IncomingRequestUi.Text(
                            sessionId = request.sessionId,
                            peerAlias = request.peer.alias,
                            text = message
                        )
                    )
                }
                return deferred.await()
            }

            addTransfers(request.files.values.toList(), TransferDirection.RECEIVING, request.sessionId)
            val settings = settingsRepository.currentData()
            if (settings.autoAcceptKnownDevices) {
                return IncomingUploadDecision.Accept(request.files.keys)
            }
            val deferred = CompletableDeferred<IncomingUploadDecision>()
            pendingDecision = deferred
            _uiState.update {
                it.copy(
                    incomingRequest = IncomingRequestUi.Files(
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
            val pending = fileStore.open(request.file, settingsRepository.currentData().saveDirectory)
            pendingOutputs["${request.sessionId}/${request.file.id}"] = pending
            return pending.output
        }

        override suspend fun onFileFinished(result: IncomingFileResult) {
            val key = "${result.sessionId}/${result.file.id}"
            val pending = pendingOutputs.remove(key)
            pending?.publish(result.success)
            if (result.success && pending != null) {
                updateReceivedLocation(result.file.id, pending.location)
            }
            updateStatus(result.file.id, if (result.success) TransferItemStatus.DONE else TransferItemStatus.FAILED)
        }

        override suspend fun onSessionCancelled(sessionId: String) {
            markInProgressCancelled(TransferDirection.RECEIVING, sessionId)
        }

        override suspend fun onRegistered(peer: LanDevice) {
            if (peer.fingerprint == localFingerprint) return
            devicesById[peer.id] = peer
            _uiState.update { it.copy(devices = devicesById.values.sortedBy { device -> device.alias }) }
        }
    }

    private data class ActiveSendSession(
        val sessionId: String,
        val device: LanDevice
    )

    private data class RetrySend(
        val device: LanDevice,
        val entry: PreparedSendEntry
    )

    private data class PreparedSendEntry(
        val dto: TransferFileDto,
        val picked: PickedFile
    )

    private companion object {
        const val TAG = "LanTransfer"
        const val MAX_UPLOAD_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 500L
        const val SCAN_INDICATOR_DURATION_MS = 3_500L
    }
}

private fun LanTransferSettings.toUi(): LanTransferSettingsUi = LanTransferSettingsUi(
    alias = alias,
    pinEnabled = pinEnabled,
    pin = pin,
    saveDirectory = saveDirectory,
    autoAccept = autoAcceptKnownDevices,
    computeChecksums = computeChecksums,
    verifyChecksums = verifyChecksums
)

private fun PickedFile.toMetadata(): FileMetadataDto? {
    val modified = modifiedAt?.let { Instant.ofEpochMilli(it).toString() }
    val accessed = accessedAt?.let { Instant.ofEpochMilli(it).toString() }
    return if (modified == null && accessed == null) null else FileMetadataDto(modified, accessed)
}
