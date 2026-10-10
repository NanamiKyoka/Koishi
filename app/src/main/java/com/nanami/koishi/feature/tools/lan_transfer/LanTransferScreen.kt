package com.nanami.koishi.feature.tools.lan_transfer

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceType
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.ContentUriFileSource

@Composable
fun LanTransferRoute(
    viewModel: LanTransferViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userMessage = state.userMessageRes?.let { res ->
        if (state.userMessageArgs.isEmpty()) stringResource(res)
        else stringResource(res, *state.userMessageArgs.toTypedArray())
    }

    LaunchedEffect(state.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(LanTransferUiEvent.OnDismissMessage)
        }
    }

    val handleUris: (List<Uri>) -> Unit = { uris ->
        val picked = uris.mapNotNull { ContentUriFileSource.describe(context.contentResolver, it) }
        if (picked.isNotEmpty()) viewModel.onEvent(LanTransferUiEvent.OnFilesPicked(picked))
    }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> handleUris(uris) }
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> handleUris(uris) }
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onEvent(LanTransferUiEvent.OnFolderPicked(uri.toString()))
        }
    }
    var pendingEnableReceive by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnableReceive) {
            pendingEnableReceive = false
            if (granted) viewModel.onEvent(LanTransferUiEvent.OnToggleReceive(true))
        }
    }

    fun requestReceiveToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.onEvent(LanTransferUiEvent.OnToggleReceive(false))
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingEnableReceive = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onEvent(LanTransferUiEvent.OnToggleReceive(true))
        }
    }

    LanTransferScreen(
        state = state,
        onBack = onBack,
        onPickFiles = { filePicker.launch(arrayOf("*/*")) },
        onPickImages = {
            mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        },
        onPickFolder = { folderPicker.launch(null) },
        onToggleReceive = ::requestReceiveToggle,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanTransferScreen(
    state: LanTransferUiState,
    onBack: () -> Unit,
    onPickFiles: () -> Unit,
    onPickImages: () -> Unit,
    onPickFolder: () -> Unit,
    onToggleReceive: (Boolean) -> Unit,
    onEvent: (LanTransferUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showAddItems by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var editingItems by remember { mutableStateOf(false) }
    var showTextTargetPicker by remember { mutableStateOf(false) }
    var textTarget by remember { mutableStateOf<LanDevice?>(null) }

    fun saveSettings(settings: LanTransferSettingsUi) {
        onEvent(
            LanTransferUiEvent.OnSaveSettings(
                alias = settings.alias,
                pinEnabled = settings.pinEnabled,
                pin = settings.pin,
                saveDirectory = settings.saveDirectory,
                autoAccept = settings.autoAccept,
                computeChecksums = settings.computeChecksums,
                verifyChecksums = settings.verifyChecksums
            )
        )
    }

    LaunchedEffect(state.selectedFiles.isEmpty()) {
        if (editingItems && state.selectedFiles.isEmpty()) editingItems = false
    }

    if (editingItems) {
        LanTransferSelectedItemsScreen(
            files = state.selectedFiles,
            onBack = { editingItems = false },
            onRemove = { onEvent(LanTransferUiEvent.OnRemoveFile(it)) },
            onClear = { onEvent(LanTransferUiEvent.OnClearFiles) },
            onUpdateText = { uri, text -> onEvent(LanTransferUiEvent.OnUpdateText(uri, text)) }
        )
    } else {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lan_transfer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.dialog_cancel)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(LanTransferUiEvent.OnRefresh) }) {
                        if (state.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.lan_transfer_refresh)
                            )
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.lan_transfer_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        AdaptiveContentWidth(maxWidth = 960.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ReceiveStatusCard(
                        enabled = state.receiveEnabled,
                        alias = state.alias.ifBlank { state.settings.alias },
                        pinEnabled = state.settings.pinEnabled && state.settings.pin.isNotBlank(),
                        pin = state.settings.pin,
                        localAddresses = state.localAddresses,
                        listenPort = state.listenPort,
                        onToggleReceive = onToggleReceive,
                        onTogglePin = { enabled ->
                            if (enabled) {
                                showPinSetup = true
                            } else {
                                saveSettings(state.settings.copy(pinEnabled = false, pin = ""))
                            }
                        },
                        onEditPin = { showPinSetup = true }
                    )
                }
                if (state.isImportingFiles) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.lan_transfer_folder_importing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    if (state.selectedFiles.isEmpty()) {
                        SendSelectionOptions(
                            onPickFiles = onPickFiles,
                            onPickMedia = onPickImages,
                            onPickText = { showTextInput = true },
                            onPickFolder = onPickFolder
                        )
                    } else {
                        SelectedItemsSummaryCard(
                            files = state.selectedFiles,
                            onClear = { onEvent(LanTransferUiEvent.OnClearFiles) },
                            onEdit = { editingItems = true },
                            onAdd = { showAddItems = true }
                        )
                    }
                }
                item {
                    DeviceSectionHeader(isScanning = state.isScanning)
                }
                if (state.devices.isEmpty()) {
                    item {
                        EmptyDevices(isScanning = state.isScanning) {
                            onEvent(LanTransferUiEvent.OnRefresh)
                        }
                    }
                } else {
                    items(state.devices, key = { it.id }) { device ->
                        DeviceRow(
                            device = device,
                            selectedFilesCount = state.selectedFiles.size,
                            activeSendDeviceId = state.activeSendDeviceId,
                            progress = state.transfers
                                .filter { it.direction == TransferDirection.SENDING && it.status == TransferItemStatus.IN_PROGRESS }
                                .takeIf { state.activeSendDeviceId == device.id }
                                ?.map { it.progress }
                                ?.average()
                                ?.toFloat(),
                            onSend = { onEvent(LanTransferUiEvent.OnSendTo(device.id)) },
                            onSendText = { textTarget = device }
                        )
                    }
                }
                if (state.transfers.isNotEmpty()) {
                    item {
                        SectionTitle(stringResource(R.string.lan_transfer_transfers_title))
                    }
                    items(state.transfers, key = { it.id }) { transfer ->
                        TransferRow(
                            item = transfer,
                            onCancel = { onEvent(LanTransferUiEvent.OnCancelTransfer(transfer.id)) },
                            onRetry = { onEvent(LanTransferUiEvent.OnRetryTransfer(transfer.id)) },
                            onDismiss = { onEvent(LanTransferUiEvent.OnDismissTransfer(transfer.id)) }
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.lan_transfer_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
    }

    state.pinPrompt?.let { prompt ->
        PinPromptDialog(
            prompt = prompt,
            onDismiss = { onEvent(LanTransferUiEvent.OnCancelPinPrompt) },
            onSubmit = { onEvent(LanTransferUiEvent.OnSubmitPin(it)) }
        )
    }

    state.incomingRequest?.let { request ->
        when (request) {
            is IncomingRequestUi.Files -> IncomingFileRequestDialog(
                request = request,
                onAccept = { onEvent(LanTransferUiEvent.OnAcceptIncoming(true)) },
                onReject = { onEvent(LanTransferUiEvent.OnAcceptIncoming(false)) }
            )

            is IncomingRequestUi.Text -> IncomingTextDialog(
                request = request,
                onCopy = { onEvent(LanTransferUiEvent.OnAcceptIncoming(true)) },
                onReject = { onEvent(LanTransferUiEvent.OnAcceptIncoming(false)) }
            )
        }
    }

    if (showTextTargetPicker) {
        SendTextTargetDialog(
            devices = state.devices,
            onDismiss = { showTextTargetPicker = false },
            onSelect = { device ->
                showTextTargetPicker = false
                textTarget = device
            }
        )
    }

    textTarget?.let { target ->
        SendTextDialog(
            deviceAlias = target.alias,
            onDismiss = { textTarget = null },
            onSend = { text ->
                textTarget = null
                onEvent(LanTransferUiEvent.OnSendText(target.id, text))
            }
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            initialPin = state.settings.pin,
            onDismiss = { showPinSetup = false },
            onSave = { pin ->
                showPinSetup = false
                saveSettings(state.settings.copy(pinEnabled = true, pin = pin))
            }
        )
    }

    if (showAddItems) {
        AddItemsBottomSheet(
            onDismiss = { showAddItems = false },
            onPickFiles = {
                showAddItems = false
                onPickFiles()
            },
            onPickMedia = {
                showAddItems = false
                onPickImages()
            },
            onPickText = {
                showAddItems = false
                showTextInput = true
            },
            onPickFolder = {
                showAddItems = false
                onPickFolder()
            }
        )
    }

    if (showTextInput) {
        TextInputDialog(
            title = stringResource(R.string.lan_transfer_preview_text),
            onDismiss = { showTextInput = false },
            onConfirm = { text ->
                showTextInput = false
                onEvent(LanTransferUiEvent.OnAddText(text))
            }
        )
    }

    if (showSettings) {
        LanTransferSettingsSheet(
            settings = state.settings,
            onDismiss = { showSettings = false },
            onSave = { settings ->
                showSettings = false
                saveSettings(settings)
            }
        )
    }
}
@Composable
private fun ReceiveStatusCard(
    enabled: Boolean,
    alias: String,
    pinEnabled: Boolean,
    pin: String,
    localAddresses: List<String>,
    listenPort: Int?,
    onToggleReceive: (Boolean) -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onEditPin: () -> Unit
) {
    val context = LocalContext.current
    val pinCopiedMessage = stringResource(R.string.lan_transfer_pin_copied)
    val addressCopiedMessage = stringResource(R.string.lan_transfer_local_address_copied)
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (enabled) R.string.lan_transfer_receive_on_status
                            else R.string.lan_transfer_receive_off_status
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = alias.ifBlank { stringResource(R.string.lan_transfer_title) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggleReceive)
            }
            HorizontalDivider(color = contentColor.copy(alpha = 0.14f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lan_transfer_local_address),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (localAddresses.isEmpty() || listenPort == null) {
                        Text(
                            text = stringResource(R.string.lan_transfer_local_address_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.72f)
                        )
                    } else {
                        Text(
                            text = localAddresses.joinToString("  ") { "$it:$listenPort" },
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.82f)
                        )
                    }
                }
                if (localAddresses.isNotEmpty() && listenPort != null) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            val address = localAddresses.joinToString("\n") { "$it:$listenPort" }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Koishi LAN Transfer Address", address))
                            Toast.makeText(
                                context,
                                addressCopiedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.lan_transfer_local_address_copied)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (pinEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (pinEnabled) R.string.lan_transfer_pin_enabled_status
                            else R.string.lan_transfer_pin_disabled_status
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (pinEnabled) {
                        Text(
                            text = pin,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (pinEnabled) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Koishi LAN Transfer PIN", pin))
                            Toast.makeText(
                                context,
                                pinCopiedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.lan_transfer_pin_copy)
                        )
                    }
                    TextButton(onClick = onEditPin) {
                        Text(stringResource(R.string.lan_transfer_pin_setup))
                    }
                }
                Switch(checked = pinEnabled, onCheckedChange = onTogglePin)
            }
            Text(
                text = stringResource(
                    if (enabled) R.string.lan_transfer_receive_on_hint
                    else R.string.lan_transfer_receive_off_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun DeviceSectionHeader(isScanning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.lan_transfer_devices_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isScanning) {
                Text(
                    text = stringResource(R.string.lan_transfer_scanning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
@Composable
private fun EmptyDevices(
    isScanning: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = stringResource(
                    if (isScanning) R.string.lan_transfer_devices_empty
                    else R.string.lan_transfer_devices_none
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRefresh, enabled = !isScanning) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.lan_transfer_refresh))
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: LanDevice,
    selectedFilesCount: Int,
    activeSendDeviceId: String?,
    progress: Float?,
    onSend: () -> Unit,
    onSendText: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isActive = activeSendDeviceId == device.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSend),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (device.deviceType == LanDeviceType.MOBILE) {
                                Icons.Rounded.PhoneAndroid
                            } else {
                                Icons.Rounded.Computer
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.alias,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.lan_transfer_device_model_protocol,
                            device.deviceModel ?: device.host,
                            device.version,
                            device.protocol.wireValue.uppercase()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lan_transfer_send_text)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSendText()
                            }
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onSend,
                    enabled = activeSendDeviceId == null || isActive
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.lan_transfer_send))
                }
            }
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (selectedFilesCount == 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.lan_transfer_no_files),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TransferRow(
    item: TransferItemUi,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val openFolderFailedMessage = stringResource(R.string.lan_transfer_open_folder_failed)
    val statusColor = when (item.status) {
        TransferItemStatus.DONE -> MaterialTheme.colorScheme.primary
        TransferItemStatus.FAILED -> MaterialTheme.colorScheme.error
        TransferItemStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        TransferItemStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.direction == TransferDirection.SENDING) {
                                Icons.AutoMirrored.Rounded.Send
                            } else {
                                Icons.Rounded.FolderOpen
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.errorMessage ?: stringResource(
                            when (item.status) {
                                TransferItemStatus.DONE -> R.string.lan_transfer_status_done
                                TransferItemStatus.FAILED -> R.string.lan_transfer_status_failed
                                TransferItemStatus.CANCELLED -> R.string.lan_transfer_status_cancelled
                                TransferItemStatus.IN_PROGRESS -> if (item.direction == TransferDirection.SENDING) {
                                    R.string.lan_transfer_status_sending
                                } else {
                                    R.string.lan_transfer_status_receiving
                                }
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.errorMessage != null) MaterialTheme.colorScheme.error else statusColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.status == TransferItemStatus.IN_PROGRESS) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.lan_transfer_cancel)
                        )
                    }
                } else if (item.canRetry) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.lan_transfer_retry)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null
                    )
                }
            }
            if (item.status == TransferItemStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val location = item.receivedLocation
            if (
                item.direction == TransferDirection.RECEIVING &&
                item.status == TransferItemStatus.DONE &&
                location != null
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = {
                        if (!LanTransferFolderOpener.open(context, location)) {
                            Toast.makeText(
                                context,
                                openFolderFailedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.lan_transfer_open_folder))
                }
            }
        }
    }
}
