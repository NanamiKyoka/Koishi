package com.nanami.koishi.feature.tools.text_apps.regex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.text_apps.common.TextEditorField
import com.nanami.koishi.feature.tools.text_apps.common.TextFileIo
import com.nanami.koishi.feature.tools.text_apps.common.TextToolTopBar
import com.nanami.koishi.feature.tools.text_apps.common.TEXT_FILE_MIME_TYPES
import com.nanami.koishi.feature.tools.text_apps.common.copyTextToClipboard
import com.nanami.koishi.feature.tools.text_apps.common.formatByteCount
import com.nanami.koishi.feature.tools.text_apps.common.readTextFromClipboard
import com.nanami.koishi.feature.tools.text_apps.common.rememberTextFileImportLauncher
import com.nanami.koishi.feature.tools.text_apps.common.textImportErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class RegexScreenMode {
    EXTRACT,
    REPLACE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegexRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(RegexScreenMode.EXTRACT) }
    var input by remember { mutableStateOf("") }
    var pattern by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf(false) }
    var dotAll by rememberSaveable { mutableStateOf(false) }
    var literal by rememberSaveable { mutableStateOf(false) }
    var replaceAll by rememberSaveable { mutableStateOf(true) }
    var extraction by remember { mutableStateOf<Result<RegexExtractionResult>?>(null) }
    var replacementResult by remember { mutableStateOf<Result<String>?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.text_tool_copied)
    val options = RegexOptions(
        ignoreCase = ignoreCase,
        multiline = multiline,
        dotAll = dotAll,
        literal = literal
    )
    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val importLauncher = rememberTextFileImportLauncher(
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
                input = text
                showMessage(context.getString(R.string.text_tool_imported, file.displayName))
            }
        },
        onError = { error -> showMessage(textImportErrorMessage(context, error)) }
    )

    LaunchedEffect(
        input,
        pattern,
        replacement,
        replaceAll,
        ignoreCase,
        multiline,
        dotAll,
        literal,
        mode
    ) {
        isProcessing = true
        val sourceInput = input
        val sourcePattern = pattern
        val sourceReplacement = replacement
        val sourceOptions = options
        delay(150)
        val nextExtraction = if (mode == RegexScreenMode.EXTRACT && sourcePattern.isNotEmpty()) {
            withContext(Dispatchers.Default) { RegexEngine.extract(sourceInput, sourcePattern, sourceOptions) }
        } else {
            null
        }
        val nextReplacement = if (mode == RegexScreenMode.REPLACE && sourcePattern.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                RegexEngine.replace(sourceInput, sourcePattern, sourceReplacement, replaceAll, sourceOptions)
            }
        } else {
            null
        }
        if (sourceInput == input && sourcePattern == pattern && sourceReplacement == replacement) {
            extraction = nextExtraction
            replacementResult = nextReplacement
        }
        isProcessing = false
    }
    val error = extraction?.exceptionOrNull() ?: replacementResult?.exceptionOrNull()
    val extractionResult = extraction?.getOrNull()
    val matches = extractionResult?.matches.orEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_regex_tool_name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(TEXT_FILE_MIME_TYPES) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = stringResource(R.string.text_tool_import_file))
                    }
                    IconButton(onClick = { input = readTextFromClipboard(context) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.text_tool_paste))
                    }
                    IconButton(onClick = {
                        input = ""
                        pattern = ""
                        replacement = ""
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 960.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == RegexScreenMode.EXTRACT,
                        onClick = { mode = RegexScreenMode.EXTRACT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.text_regex_extract))
                    }
                    SegmentedButton(
                        selected = mode == RegexScreenMode.REPLACE,
                        onClick = { mode = RegexScreenMode.REPLACE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.text_regex_replace))
                    }
                }

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.text_regex_pattern)) },
                    placeholder = { Text(stringResource(R.string.text_regex_pattern_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    isError = error != null,
                    shape = MaterialTheme.shapes.medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ignoreCase,
                        onClick = { ignoreCase = !ignoreCase },
                        label = { Text(stringResource(R.string.text_regex_ignore_case)) }
                    )
                    FilterChip(
                        selected = multiline,
                        onClick = { multiline = !multiline },
                        label = { Text(stringResource(R.string.text_regex_multiline)) }
                    )
                    FilterChip(
                        selected = dotAll,
                        onClick = { dotAll = !dotAll },
                        label = { Text(stringResource(R.string.text_regex_dot_all)) }
                    )
                    FilterChip(
                        selected = literal,
                        onClick = { literal = !literal },
                        label = { Text(stringResource(R.string.text_regex_literal)) }
                    )
                }

                if (mode == RegexScreenMode.REPLACE) {
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.text_regex_replacement)) },
                        placeholder = { Text(stringResource(R.string.text_regex_replacement_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        shape = MaterialTheme.shapes.medium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = replaceAll,
                            onClick = { replaceAll = true },
                            label = { Text(stringResource(R.string.text_regex_all_matches)) }
                        )
                        FilterChip(
                            selected = !replaceAll,
                            onClick = { replaceAll = false },
                            label = { Text(stringResource(R.string.text_regex_first_match)) }
                        )
                    }
                }

                TextEditorField(
                    value = input,
                    onValueChange = { input = it },
                    label = stringResource(R.string.text_tool_input),
                    placeholder = stringResource(R.string.text_regex_input_placeholder),
                    minLines = 7,
                    maxLines = 12,
                    isError = error != null
                )

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                error?.let {
                    Text(
                        text = "${stringResource(R.string.text_regex_invalid)}: ${it.message.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                when (mode) {
                    RegexScreenMode.EXTRACT -> RegexExtractionSection(
                        result = extractionResult ?: RegexExtractionResult(emptyList(), 0),
                        onCopy = { value ->
                            copyTextToClipboard(context, value)
                            scope.launch { snackbarHostState.showSnackbar(copied) }
                        }
                    )

                    RegexScreenMode.REPLACE -> {
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
                            IconButton(
                                onClick = {
                                    replacementResult?.getOrNull()?.let { value ->
                                        copyTextToClipboard(context, value)
                                        scope.launch { snackbarHostState.showSnackbar(copied) }
                                    }
                                },
                                enabled = replacementResult?.isSuccess == true
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                            }
                        }
                        TextEditorField(
                            value = replacementResult?.getOrDefault("").orEmpty(),
                            onValueChange = {},
                            label = stringResource(R.string.text_tool_result),
                            placeholder = stringResource(R.string.text_regex_result_placeholder),
                            readOnly = true,
                            isError = error != null,
                            minLines = 7,
                            maxLines = 12
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegexExtractionSection(
    result: RegexExtractionResult,
    onCopy: (String) -> Unit
) {
    val matches = result.matches
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (matches.isEmpty()) {
                    stringResource(R.string.text_regex_no_matches)
                } else {
                    stringResource(R.string.text_regex_match_count, result.matchCount)
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (matches.isNotEmpty()) {
                IconButton(onClick = { onCopy(matches.joinToString("\n") { it.value }) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                }
            }
        }

        matches.take(200).forEachIndexed { index, match ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.text_regex_position, match.start, match.end),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { onCopy(match.value) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = match.value,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    match.groups.drop(1).forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.text_regex_group, group.index),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SelectionContainer {
                                Text(
                                    text = group.value.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (result.matchCount > matches.size) {
            Text(
                text = stringResource(R.string.text_regex_truncated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
