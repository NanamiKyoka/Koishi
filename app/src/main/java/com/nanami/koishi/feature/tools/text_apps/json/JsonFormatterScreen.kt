package com.nanami.koishi.feature.tools.text_apps.json

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.core.designsystem.adaptive.LocalWindowLayoutMode
import com.nanami.koishi.core.designsystem.adaptive.WindowLayoutMode
import com.nanami.koishi.feature.tools.text_apps.common.ImportedTextFile
import com.nanami.koishi.feature.tools.text_apps.common.TextEditorField
import com.nanami.koishi.feature.tools.text_apps.common.TextFileIo
import com.nanami.koishi.feature.tools.text_apps.common.TextImportErrorKind
import com.nanami.koishi.feature.tools.text_apps.common.TextImportException
import com.nanami.koishi.feature.tools.text_apps.common.TextToolLargeFileCard
import com.nanami.koishi.feature.tools.text_apps.common.TextToolTopBar
import com.nanami.koishi.feature.tools.text_apps.common.TEXT_FILE_MIME_TYPES
import com.nanami.koishi.feature.tools.text_apps.common.copyTextToClipboard
import com.nanami.koishi.feature.tools.text_apps.common.formatByteCount
import com.nanami.koishi.feature.tools.text_apps.common.readTextFromClipboard
import com.nanami.koishi.feature.tools.text_apps.common.rememberTextFileImportLauncher
import com.nanami.koishi.feature.tools.text_apps.common.textImportErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JsonFormatterRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var importedFile by remember { mutableStateOf<ImportedTextFile?>(null) }
    var largeOutputFile by remember { mutableStateOf<File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var indentSize by rememberSaveable { mutableIntStateOf(2) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val invalidJson = stringResource(R.string.text_tool_invalid_json)
    val validJson = stringResource(R.string.text_tool_valid_json)
    val copied = stringResource(R.string.text_tool_copied)
    val exported = stringResource(R.string.text_tool_exported)
    val exportFailed = stringResource(R.string.text_tool_export_failed)
    val processing = stringResource(R.string.text_tool_processing)
    val largeInput = importedFile?.takeIf { !it.isInline }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun clear() {
        largeOutputFile?.delete()
        input = ""
        output = ""
        errorDetail = null
        importedFile = null
        largeOutputFile = null
    }

    fun acceptImportedFile(file: ImportedTextFile) {
        largeOutputFile?.delete()
        importedFile = file
        input = file.inlineText.orEmpty()
        output = ""
        errorDetail = null
        largeOutputFile = null
        showMessage(context.getString(R.string.text_tool_imported, file.displayName))
    }

    val importLauncher = rememberTextFileImportLauncher(
        onImported = ::acceptImportedFile,
        onError = { error -> showMessage(textImportErrorMessage(context, error)) }
    )

    suspend fun importSharedUri(uri: Uri) {
        try {
            acceptImportedFile(TextFileIo.importDocument(context, uri))
        } catch (error: Throwable) {
            val importError = error as? TextImportException
                ?: TextImportException(TextImportErrorKind.READ_FAILED, cause = error)
            showMessage(textImportErrorMessage(context, importError))
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            TextFileIo.cleanupCacheFiles(context, "koishi-json-")
        }
        while (true) {
            val uri = JsonShareStore.consume() ?: break
            importSharedUri(uri)
        }
        JsonShareStore.events.collect {
            val uri = JsonShareStore.consume() ?: return@collect
            importSharedUri(uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val file = largeOutputFile
                if (file != null) {
                    TextFileIo.copyToFile(file, context, uri)
                } else {
                    TextFileIo.writeText(context, uri, output)
                }
            }
            showMessage(if (result.isSuccess) exported else exportFailed)
        }
    }

    fun exportResult() {
        if (largeOutputFile == null && output.isEmpty()) return
        val baseName = importedFile?.displayName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "formatted"
        exportLauncher.launch("$baseName.json")
    }

    fun processLargeJson(mode: JsonFileMode) {
        val file = largeInput ?: return
        scope.launch {
            isProcessing = true
            largeOutputFile?.delete()
            val target = if (mode == JsonFileMode.VALIDATE) {
                null
            } else {
                TextFileIo.createCacheFile(context, "koishi-json-", ".json")
            }
            val result = JsonFileEngine.process(
                context = context,
                inputUri = file.uri,
                outputFile = target,
                mode = mode,
                indentSize = indentSize
            )
            isProcessing = false
            result.onSuccess { processed ->
                errorDetail = null
                largeOutputFile = processed
                if (mode == JsonFileMode.VALIDATE) showMessage(validJson)
            }.onFailure { error ->
                largeOutputFile = null
                errorDetail = error.message
                if (mode == JsonFileMode.VALIDATE) showMessage(invalidJson)
            }
        }
    }

    fun applyFormat(minify: Boolean) {
        if (largeInput != null) {
            processLargeJson(if (minify) JsonFileMode.MINIFY else JsonFileMode.FORMAT)
            return
        }

        val source = input
        errorDetail = null
        largeOutputFile?.delete()
        largeOutputFile = null
        scope.launch {
            isProcessing = true
            val result = withContext(Dispatchers.Default) {
                if (minify) JsonFormatterEngine.minify(source) else JsonFormatterEngine.format(source, indentSize)
            }
            if (input == source) {
                result.onSuccess { formatted ->
                    if (formatted.length > TextFileIo.RESULT_INLINE_LIMIT_BYTES) {
                        val file = withContext(Dispatchers.IO) {
                            TextFileIo.createCacheFile(context, "koishi-json-", ".json").apply {
                                writeText(formatted)
                            }
                        }
                        output = ""
                        largeOutputFile = file
                    } else {
                        output = formatted
                    }
                    errorDetail = null
                }.onFailure { error ->
                    output = ""
                    errorDetail = error.message
                }
            }
            isProcessing = false
        }
    }

    fun validate() {
        if (largeInput != null) {
            processLargeJson(JsonFileMode.VALIDATE)
            return
        }

        val source = input
        scope.launch {
            isProcessing = true
            val result = withContext(Dispatchers.Default) { JsonFormatterEngine.minify(source) }
            if (input == source) {
                errorDetail = result.exceptionOrNull()?.message
                showMessage(if (result.isSuccess) validJson else invalidJson)
            }
            isProcessing = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_json_formatter_name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(TEXT_FILE_MIME_TYPES) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = stringResource(R.string.text_tool_import_file))
                    }
                    IconButton(onClick = {
                        input = readTextFromClipboard(context)
                        output = ""
                        errorDetail = null
                        importedFile = null
                        largeOutputFile = null
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.text_tool_paste))
                    }
                    IconButton(onClick = ::clear) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 1100.dp) {
            val layoutMode = LocalWindowLayoutMode.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            enabled = !isProcessing && (input.isNotEmpty() || largeInput != null),
                            onClick = { applyFormat(false) }
                        ) {
                            Text(stringResource(R.string.text_tool_format))
                        }
                        OutlinedButton(
                            enabled = !isProcessing && (input.isNotEmpty() || largeInput != null),
                            onClick = { applyFormat(true) }
                        ) {
                            Text(stringResource(R.string.text_tool_minify))
                        }
                        TextButton(
                            enabled = !isProcessing && (input.isNotEmpty() || largeInput != null),
                            onClick = ::validate
                        ) {
                            Text(stringResource(R.string.text_tool_validate))
                        }
                        if (isProcessing) {
                            Text(
                                text = processing,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = indentSize == 2,
                            enabled = !isProcessing,
                            onClick = { indentSize = 2 },
                            label = { Text(stringResource(R.string.text_tool_indent_spaces, 2)) }
                        )
                        FilterChip(
                            selected = indentSize == 4,
                            enabled = !isProcessing,
                            onClick = { indentSize = 4 },
                            label = { Text(stringResource(R.string.text_tool_indent_spaces, 4)) }
                        )
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (layoutMode == WindowLayoutMode.EXPANDED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (largeInput != null) {
                                TextToolLargeFileCard(file = largeInput)
                            } else {
                                JsonInputField(
                                    value = input,
                                    onValueChange = {
                                        input = it
                                        errorDetail = null
                                        importedFile = null
                                        largeOutputFile = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        JsonOutputField(
                            value = output,
                            largeOutputFile = largeOutputFile,
                            errorDetail = errorDetail,
                            onCopy = {
                                copyTextToClipboard(context, output)
                                showMessage(copied)
                            },
                            onExport = ::exportResult,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    if (largeInput != null) {
                        TextToolLargeFileCard(file = largeInput)
                    } else {
                        JsonInputField(
                            value = input,
                            onValueChange = {
                                input = it
                                errorDetail = null
                                importedFile = null
                                largeOutputFile = null
                            }
                        )
                    }
                    JsonOutputField(
                        value = output,
                        largeOutputFile = largeOutputFile,
                        errorDetail = errorDetail,
                        onCopy = {
                            copyTextToClipboard(context, output)
                            showMessage(copied)
                        },
                        onExport = ::exportResult
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun JsonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextEditorField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.text_tool_input),
        placeholder = stringResource(R.string.text_tool_json_placeholder),
        modifier = modifier,
        minLines = 12,
        maxLines = 18
    )
}

@Composable
private fun JsonOutputField(
    value: String,
    largeOutputFile: File?,
    errorDetail: String?,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.text_tool_result),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                IconButton(onClick = onExport, enabled = largeOutputFile != null || value.isNotEmpty()) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.text_tool_export))
                }
                IconButton(onClick = onCopy, enabled = value.isNotEmpty()) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                }
            }
        }
        if (largeOutputFile != null) {
            LargeOutputCard(file = largeOutputFile, onExport = onExport)
        } else {
            TextEditorField(
                value = value,
                onValueChange = {},
                label = stringResource(R.string.text_tool_output),
                placeholder = stringResource(R.string.text_tool_json_output_placeholder),
                readOnly = true,
                isError = errorDetail != null,
                minLines = 10,
                maxLines = 16
            )
        }
        errorDetail?.let { detail ->
            Text(
                text = "${stringResource(R.string.text_tool_invalid_json)}: $detail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun LargeOutputCard(file: File, onExport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.text_tool_large_output),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        R.string.text_tool_file_metadata,
                        file.name,
                        formatByteCount(file.length())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.text_tool_export))
            }
        }
    }
}
