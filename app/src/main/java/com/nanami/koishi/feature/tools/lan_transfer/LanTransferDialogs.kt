package com.nanami.koishi.feature.tools.lan_transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDevice
import com.nanami.koishi.feature.tools.lan_transfer.engine.model.LanDeviceType
import java.security.SecureRandom
import java.util.Locale

@Composable
internal fun PinSetupDialog(
    initialPin: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember(initialPin) {
        mutableStateOf(initialPin.ifBlank { generatePin() })
    }
    var showLengthError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lan_transfer_pin_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.lan_transfer_pin_setup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filterNot(Char::isWhitespace).uppercase(Locale.US).take(16)
                        showLengthError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.lan_transfer_pin)) },
                    singleLine = true,
                    isError = showLengthError,
                    supportingText = if (showLengthError) {
                        { Text(stringResource(R.string.lan_transfer_pin_length_error)) }
                    } else {
                        null
                    },
                    trailingIcon = {
                        IconButton(onClick = { pin = generatePin() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.lan_transfer_pin_generate)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalized = pin.trim()
                    if (normalized.length < 4) {
                        showLengthError = true
                    } else {
                        onSave(normalized)
                    }
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
internal fun PinPromptDialog(
    prompt: PinPromptUi,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pin by remember(prompt.deviceId, prompt.attempt) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lan_transfer_pin_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.lan_transfer_pin_prompt_desc, prompt.deviceAlias),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filterNot(Char::isWhitespace).uppercase(Locale.US).take(16) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.lan_transfer_pin)) },
                    singleLine = true,
                    isError = prompt.invalid,
                    supportingText = if (prompt.invalid) {
                        { Text(stringResource(R.string.lan_transfer_pin_prompt_invalid)) }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(pin.trim()) },
                enabled = pin.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanTransferSettingsSheet(
    settings: LanTransferSettingsUi,
    onDismiss: () -> Unit,
    onSave: (LanTransferSettingsUi) -> Unit
) {
    var alias by remember(settings.alias) { mutableStateOf(settings.alias) }
    var saveDirectory by remember(settings.saveDirectory) { mutableStateOf(settings.saveDirectory) }
    var autoAccept by remember(settings.autoAccept) { mutableStateOf(settings.autoAccept) }
    var computeChecksums by remember(settings.computeChecksums) { mutableStateOf(settings.computeChecksums) }
    var verifyChecksums by remember(settings.verifyChecksums) { mutableStateOf(settings.verifyChecksums) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.lan_transfer_settings_advanced),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.lan_transfer_device_name)) },
                singleLine = true
            )
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
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            alias = alias,
                            saveDirectory = saveDirectory,
                            autoAccept = autoAccept,
                            computeChecksums = computeChecksums,
                            verifyChecksums = verifyChecksums
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }
    }
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
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun SendTextTargetDialog(
    devices: List<LanDevice>,
    onDismiss: () -> Unit,
    onSelect: (LanDevice) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lan_transfer_text_target_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.lan_transfer_text_target_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (devices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lan_transfer_devices_none),
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(device) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (device.deviceType == LanDeviceType.MOBILE) {
                                    Icons.Rounded.PhoneAndroid
                                } else {
                                    Icons.Rounded.Computer
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column {
                                Text(device.alias)
                                Text(
                                    text = device.deviceModel ?: device.host,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
internal fun SendTextDialog(
    deviceAlias: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lan_transfer_send_text)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.lan_transfer_send_text_to, deviceAlias),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.lan_transfer_text_hint)) },
                    minLines = 3,
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(text) },
                enabled = text.isNotBlank()
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
internal fun IncomingFileRequestDialog(
    request: IncomingRequestUi.Files,
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

@Composable
internal fun IncomingTextDialog(
    request: IncomingRequestUi.Text,
    onCopy: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.lan_transfer_text_copied)

    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(stringResource(R.string.lan_transfer_incoming_text_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.lan_transfer_incoming_text_from,
                        request.peerAlias
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        text = request.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Koishi LAN Transfer", request.text))
                    Toast.makeText(
                        context,
                        copiedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                    onCopy()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.lan_transfer_copy_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(R.string.lan_transfer_reject))
            }
        }
    )
}

internal fun formatBytes(bytes: Long): String {
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

private fun generatePin(): String = buildString(PIN_LENGTH) {
    repeat(PIN_LENGTH) {
        append(PIN_ALPHABET[secureRandom.nextInt(PIN_ALPHABET.length)])
    }
}

private val secureRandom = SecureRandom()
private const val PIN_LENGTH = 6
private const val PIN_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
