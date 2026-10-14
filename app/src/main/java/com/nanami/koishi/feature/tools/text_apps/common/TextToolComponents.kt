package com.nanami.koishi.feature.tools.text_apps.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import kotlinx.coroutines.launch

fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Koishi text tool", text))
}

fun readTextFromClipboard(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context).toString()
}

@Composable
fun rememberTextFileImportLauncher(
    inlineLimitBytes: Long = TextFileIo.DEFAULT_INLINE_LIMIT_BYTES,
    onImported: (ImportedTextFile) -> Unit,
    onError: (TextImportException) -> Unit
): ActivityResultLauncher<Array<String>> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnImported = rememberUpdatedState(onImported)
    val currentOnError = rememberUpdatedState(onError)

    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                TextFileIo.importDocument(context, uri, inlineLimitBytes)
            }.onSuccess {
                currentOnImported.value(it)
            }.onFailure {
                currentOnError.value(
                    it as? TextImportException
                        ?: TextImportException(TextImportErrorKind.READ_FAILED, cause = it)
                )
            }
        }
    }
}

fun textImportErrorMessage(context: Context, error: TextImportException): String = when (error.kind) {
    TextImportErrorKind.TOO_LARGE -> context.getString(
        R.string.text_tool_file_too_large,
        formatByteCount(error.byteLimit ?: TextFileIo.DEFAULT_INLINE_LIMIT_BYTES)
    )

    TextImportErrorKind.UNSUPPORTED_ENCODING -> context.getString(R.string.text_tool_unsupported_encoding)
    TextImportErrorKind.READ_FAILED -> context.getString(R.string.text_tool_import_failed)
}

fun formatByteCount(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MiB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(java.util.Locale.US, "%.0f KiB", bytes / 1024.0)
    else -> "$bytes B"
}

val TEXT_FILE_MIME_TYPES = arrayOf(
    "application/json",
    "text/*",
    "application/xml",
    "application/octet-stream"
)

const val MAX_INLINE_TEXT_CHARS = 1024 * 1024

@Composable
fun TextToolLargeFileCard(
    file: ImportedTextFile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.text_tool_large_file),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(
                    R.string.text_tool_file_metadata,
                    file.byteCount?.let(::formatByteCount) ?: stringResource(R.string.text_tool_unknown_size),
                    stringResource(R.string.text_tool_large_file_desc)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToolTopBar(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(stringResource(titleRes)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun TextEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    isError: Boolean = false,
    minLines: Int = 6,
    maxLines: Int = 12,
    maxChars: Int = MAX_INLINE_TEXT_CHARS
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            if (updated.length <= maxChars) {
                onValueChange(updated)
            } else {
                Toast.makeText(context, R.string.text_tool_text_too_large, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { hint -> { Text(hint) } },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        readOnly = readOnly,
        isError = isError,
        minLines = minLines,
        maxLines = maxLines,
        shape = MaterialTheme.shapes.medium
    )
}
