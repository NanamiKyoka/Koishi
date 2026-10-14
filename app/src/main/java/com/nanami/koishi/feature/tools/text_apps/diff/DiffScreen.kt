package com.nanami.koishi.feature.tools.text_apps.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.core.designsystem.adaptive.LocalWindowLayoutMode
import com.nanami.koishi.core.designsystem.adaptive.WindowLayoutMode
import com.nanami.koishi.feature.tools.text_apps.common.TextEditorField
import com.nanami.koishi.feature.tools.text_apps.common.TextFileIo
import com.nanami.koishi.feature.tools.text_apps.common.TextToolTopBar
import com.nanami.koishi.feature.tools.text_apps.common.TEXT_FILE_MIME_TYPES
import com.nanami.koishi.feature.tools.text_apps.common.copyTextToClipboard
import com.nanami.koishi.feature.tools.text_apps.common.formatByteCount
import com.nanami.koishi.feature.tools.text_apps.common.rememberTextFileImportLauncher
import com.nanami.koishi.feature.tools.text_apps.common.textImportErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DiffRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var oldText by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var ignoreWhitespace by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.text_tool_copied)
    var result by remember { mutableStateOf<Result<DiffResult>?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val oldImportLauncher = rememberTextFileImportLauncher(
        onImported = { file ->
            val text = file.inlineText
            if (text == null) {
                showMessage(
                    context.getString(
                        R.string.text_tool_file_too_large,
                        formatByteCount(TextFileIo.DEFAULT_INLINE_LIMIT_BYTES)
                    )
                )
            } else {
                oldText = text
                showMessage(context.getString(R.string.text_tool_imported, file.displayName))
            }
        },
        onError = { error -> showMessage(textImportErrorMessage(context, error)) }
    )

    val newImportLauncher = rememberTextFileImportLauncher(
        onImported = { file ->
            val text = file.inlineText
            if (text == null) {
                showMessage(
                    context.getString(
                        R.string.text_tool_file_too_large,
                        formatByteCount(TextFileIo.DEFAULT_INLINE_LIMIT_BYTES)
                    )
                )
            } else {
                newText = text
                showMessage(context.getString(R.string.text_tool_imported, file.displayName))
            }
        },
        onError = { error -> showMessage(textImportErrorMessage(context, error)) }
    )

    LaunchedEffect(oldText, newText, ignoreCase, ignoreWhitespace) {
        isProcessing = true
        val sourceOld = oldText
        val sourceNew = newText
        delay(200)
        val next = withContext(Dispatchers.Default) {
            DiffEngine.diff(sourceOld, sourceNew, ignoreCase, ignoreWhitespace)
        }
        if (sourceOld == oldText && sourceNew == newText) {
            result = next
        }
        isProcessing = false
    }

    val diffResult = result?.getOrNull()
    val error = result?.exceptionOrNull()
    val errorText = when (error) {
        is DiffLimitExceededException -> stringResource(R.string.text_diff_limit)
        null -> null
        else -> stringResource(R.string.text_diff_failed)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_text_diff_name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        oldText = ""
                        newText = ""
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 1200.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "options") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = ignoreCase,
                                onClick = { ignoreCase = !ignoreCase },
                                label = { Text(stringResource(R.string.text_diff_ignore_case)) }
                            )
                            FilterChip(
                                selected = ignoreWhitespace,
                                onClick = { ignoreWhitespace = !ignoreWhitespace },
                                label = { Text(stringResource(R.string.text_diff_ignore_whitespace)) }
                            )
                        }
                    }
                }

                item(key = "inputs") {
                    val layoutMode = LocalWindowLayoutMode.current
                    if (layoutMode == WindowLayoutMode.EXPANDED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            DiffInput(
                                value = oldText,
                                onValueChange = { oldText = it },
                                label = stringResource(R.string.text_diff_original),
                                placeholder = stringResource(R.string.text_diff_original_placeholder),
                                onImport = { oldImportLauncher.launch(TEXT_FILE_MIME_TYPES) },
                                modifier = Modifier.weight(1f)
                            )
                            DiffInput(
                                value = newText,
                                onValueChange = { newText = it },
                                label = stringResource(R.string.text_diff_modified),
                                placeholder = stringResource(R.string.text_diff_modified_placeholder),
                                onImport = { newImportLauncher.launch(TEXT_FILE_MIME_TYPES) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DiffInput(
                                value = oldText,
                                onValueChange = { oldText = it },
                                label = stringResource(R.string.text_diff_original),
                                placeholder = stringResource(R.string.text_diff_original_placeholder),
                                onImport = { oldImportLauncher.launch(TEXT_FILE_MIME_TYPES) }
                            )
                            DiffInput(
                                value = newText,
                                onValueChange = { newText = it },
                                label = stringResource(R.string.text_diff_modified),
                                placeholder = stringResource(R.string.text_diff_modified_placeholder),
                                onImport = { newImportLauncher.launch(TEXT_FILE_MIME_TYPES) }
                            )
                        }
                    }
                }

                if (isProcessing) {
                    item(key = "processing") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                errorText?.let { message ->
                    item(key = "error") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                diffResult?.let { diff ->
                    item(key = "summary") {
                        DiffSummary(
                            result = diff,
                            onCopy = {
                                copyTextToClipboard(context, diff.toUnifiedText())
                                scope.launch { snackbarHostState.showSnackbar(copied) }
                            }
                        )
                    }

                    itemsIndexed(
                        items = diff.lines,
                        key = { index, _ -> index }
                    ) { _, line ->
                        DiffLineRow(line)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onImport) {
                Icon(Icons.Rounded.UploadFile, contentDescription = stringResource(R.string.text_tool_import_file))
            }
        }
        TextEditorField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            minLines = 6,
            maxLines = 10
        )
    }
}

@Composable
private fun DiffSummary(
    result: DiffResult,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (result.additions == 0 && result.deletions == 0) {
                Text(
                    text = stringResource(R.string.text_diff_identical),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.text_diff_additions, result.additions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(R.string.text_diff_deletions, result.deletions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.text_diff_unchanged, result.lines.count { it.kind == DiffLineKind.EQUAL }),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val containerColor = when (line.kind) {
        DiffLineKind.EQUAL -> MaterialTheme.colorScheme.surfaceContainer
        DiffLineKind.INSERT -> MaterialTheme.colorScheme.tertiaryContainer
        DiffLineKind.DELETE -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (line.kind) {
        DiffLineKind.EQUAL -> MaterialTheme.colorScheme.onSurfaceVariant
        DiffLineKind.INSERT -> MaterialTheme.colorScheme.onTertiaryContainer
        DiffLineKind.DELETE -> MaterialTheme.colorScheme.onErrorContainer
    }
    val marker = when (line.kind) {
        DiffLineKind.EQUAL -> " "
        DiffLineKind.INSERT -> "+"
        DiffLineKind.DELETE -> "-"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = line.oldLineNumber?.toString().orEmpty(),
            modifier = Modifier.width(30.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = contentColor,
            textAlign = TextAlign.End
        )
        Text(
            text = line.newLineNumber?.toString().orEmpty(),
            modifier = Modifier.width(30.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = contentColor,
            textAlign = TextAlign.End
        )
        Text(
            text = marker,
            modifier = Modifier.width(14.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = contentColor
        )
        Text(
            text = line.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = contentColor
        )
    }
}

private fun DiffResult.toUnifiedText(): String = lines.joinToString("\n") { line ->
    val marker = when (line.kind) {
        DiffLineKind.EQUAL -> " "
        DiffLineKind.INSERT -> "+"
        DiffLineKind.DELETE -> "-"
    }
    "$marker${line.text}"
}
