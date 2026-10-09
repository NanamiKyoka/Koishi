package com.nanami.koishi.feature.tools.lan_transfer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
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

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val resolver = context.contentResolver
        val picked = uris.mapNotNull { ContentUriFileSource.describe(resolver, it) }
        viewModel.onEvent(LanTransferUiEvent.OnFilesPicked(picked))
    }

    var pendingEnableReceive by remember { mutableStateOf(false) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEnableReceive) {
            pendingEnableReceive = false
            if (granted) viewModel.onEvent(LanTransferUiEvent.OnToggleReceive(true))
        }
    }
    val requestToggle: (Boolean) -> Unit = { enabled ->
        val needsPermission = enabled &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingEnableReceive = true
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.onEvent(LanTransferUiEvent.OnToggleReceive(enabled))
        }
    }

    LanTransferScreen(
        state = state,
        onBack = onBack,
        onEvent = { event ->
            if (event is LanTransferUiEvent.OnToggleReceive) requestToggle(event.enabled)
            else viewModel.onEvent(event)
        },
        onPickFiles = { filePicker.launch(arrayOf("*/*")) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanTransferScreen(
    state: LanTransferUiState,
    onBack: () -> Unit,
    onEvent: (LanTransferUiEvent) -> Unit,
    onPickFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.lan_transfer_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ReceiveToggleCard(
                    enabled = state.receiveEnabled,
                    onToggle = { onEvent(LanTransferUiEvent.OnToggleReceive(it)) }
                )
            }
            item {
                DeviceSectionHeader(onRefresh = { onEvent(LanTransferUiEvent.OnRefresh) })
            }
            if (state.devices.isEmpty()) {
                item {
                    EmptyDevices(isLoading = state.isLoading)
                }
            } else {
                items(state.devices, key = { it.id }) { device ->
                    DeviceRow(device = device, onSend = { onEvent(LanTransferUiEvent.OnSendTo(device.id)) })
                }
            }
            item {
                SendFilesCard(
                    filesCount = state.selectedFiles.size,
                    names = state.selectedFiles.map { it.fileName },
                    onPickFiles = onPickFiles
                )
            }
            if (state.transfers.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.lan_transfer_transfers_title)) }
                items(state.transfers, key = { it.id }) { transfer ->
                    TransferRow(
                        item = transfer,
                        onCancel = { onEvent(LanTransferUiEvent.OnCancelTransfer(transfer.id)) },
                        onRetry = { onEvent(LanTransferUiEvent.OnRetryTransfer(transfer.id)) },
                        onDismiss = { onEvent(LanTransferUiEvent.OnDismissTransfer(transfer.id)) }
                    )
                }
            }
        }
    }

    state.incomingRequest?.let { request ->
        IncomingRequestDialog(
            request = request,
            onAccept = { onEvent(LanTransferUiEvent.OnAcceptIncoming(true)) },
            onReject = { onEvent(LanTransferUiEvent.OnAcceptIncoming(false)) }
        )
    }

    if (showSettings) {
        LanTransferSettingsDialog(
            settings = state.settings,
            onDismiss = { showSettings = false },
            onSave = { settings ->
                showSettings = false
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
        )
    }
}

@Composable
private fun ReceiveToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lan_transfer_receive),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.lan_transfer_receive_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun DeviceSectionHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.lan_transfer_devices_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = stringResource(R.string.lan_transfer_refresh))
        }
    }
}

@Composable
private fun EmptyDevices(isLoading: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = stringResource(R.string.lan_transfer_devices_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(device: LanDevice, onSend: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (device.deviceType == com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceType.MOBILE) {
                    Icons.Rounded.PhoneAndroid
                } else {
                    Icons.Rounded.Computer
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.alias,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.lan_transfer_send_files))
            }
        }
    }
}

@Composable
private fun SendFilesCard(filesCount: Int, names: List<String>, onPickFiles: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = onPickFiles,
                    label = { Text(stringResource(R.string.lan_transfer_send_files)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                if (filesCount > 0) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.lan_transfer_files_selected, filesCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (names.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = names.joinToString("、"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = when (item.status) {
                        TransferItemStatus.DONE -> Icons.Rounded.Check
                        TransferItemStatus.FAILED -> Icons.Rounded.ErrorOutline
                        TransferItemStatus.CANCELLED -> Icons.Rounded.Cancel
                        TransferItemStatus.IN_PROGRESS -> Icons.Rounded.Add
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when (item.status) {
                        TransferItemStatus.DONE -> MaterialTheme.colorScheme.primary
                        TransferItemStatus.FAILED -> MaterialTheme.colorScheme.error
                        TransferItemStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        TransferItemStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (item.status == TransferItemStatus.IN_PROGRESS) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(R.string.lan_transfer_cancel),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (item.canRetry) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.lan_transfer_retry),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (item.status == TransferItemStatus.IN_PROGRESS) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                HorizontalDivider(thickness = 1.dp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanTransferSettingsDialog(
    settings: LanTransferSettingsUi,
    onDismiss: () -> Unit,
    onSave: (LanTransferSettingsUi) -> Unit
) {
    var alias by remember(settings.alias) { mutableStateOf(settings.alias) }
    var pinEnabled by remember(settings.pinEnabled) { mutableStateOf(settings.pinEnabled) }
    var pin by remember(settings.pin) { mutableStateOf(settings.pin) }
    var saveDirectory by remember(settings.saveDirectory) { mutableStateOf(settings.saveDirectory) }
    var autoAccept by remember(settings.autoAccept) { mutableStateOf(settings.autoAccept) }
    var computeChecksums by remember(settings.computeChecksums) { mutableStateOf(settings.computeChecksums) }
    var verifyChecksums by remember(settings.verifyChecksums) { mutableStateOf(settings.verifyChecksums) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lan_transfer_settings)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.lan_transfer_device_name)) },
                    singleLine = true
                )
                SettingsToggleRow(
                    title = stringResource(R.string.lan_transfer_pin_enabled),
                    checked = pinEnabled,
                    onCheckedChange = { pinEnabled = it }
                )
                if (pinEnabled) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.lan_transfer_pin)) },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = saveDirectory,
                    onValueChange = { saveDirectory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.lan_transfer_save_directory)) },
                    singleLine = true
                )
                SettingsToggleRow(
                    title = stringResource(R.string.lan_transfer_auto_accept),
                    checked = autoAccept,
                    onCheckedChange = { autoAccept = it }
                )
                SettingsToggleRow(
                    title = stringResource(R.string.lan_transfer_compute_checksums),
                    checked = computeChecksums,
                    onCheckedChange = { computeChecksums = it }
                )
                SettingsToggleRow(
                    title = stringResource(R.string.lan_transfer_verify_checksums),
                    checked = verifyChecksums,
                    onCheckedChange = { verifyChecksums = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        LanTransferSettingsUi(
                            alias = alias,
                            pinEnabled = pinEnabled,
                            pin = pin,
                            saveDirectory = saveDirectory,
                            autoAccept = autoAccept,
                            computeChecksums = computeChecksums,
                            verifyChecksums = verifyChecksums
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IncomingRequestDialog(
    request: IncomingRequestUi,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(stringResource(R.string.lan_transfer_incoming_title)) },
        text = {
            Text(
                stringResource(
                    R.string.lan_transfer_incoming_desc,
                    request.peerAlias,
                    request.fileCount,
                    formatBytes(request.totalBytes)
                )
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(R.string.lan_transfer_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(R.string.lan_transfer_reject))
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }
    return "%.1f %s".format(value, units[unitIndex])
}
