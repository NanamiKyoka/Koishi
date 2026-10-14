package com.nanami.koishi.feature.tools.text_apps.text_process

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

private enum class TextProcessTab(@StringRes val labelRes: Int) {
    CASE(R.string.text_process_case),
    DEDUPLICATE(R.string.text_process_deduplicate),
    SORT(R.string.text_process_sort),
    STATISTICS(R.string.text_process_statistics)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TextProcessRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(TextProcessTab.CASE) }
    var caseMode by rememberSaveable { mutableStateOf(CaseMode.LOWERCASE) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.ASCENDING) }
    var trimLines by rememberSaveable { mutableStateOf(false) }
    var removeEmpty by rememberSaveable { mutableStateOf(false) }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var statistics by remember { mutableStateOf(TextProcessEngine.statistics("")) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.text_tool_copied)
    val options = TextProcessOptions(
        trimLines = trimLines,
        removeEmpty = removeEmpty,
        ignoreCase = ignoreCase
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

    LaunchedEffect(input, tab, caseMode, sortMode, trimLines, removeEmpty, ignoreCase) {
        isProcessing = true
        val source = input
        delay(150)
        val result = withContext(Dispatchers.Default) {
            val nextOutput = when (tab) {
                TextProcessTab.CASE -> TextProcessEngine.transformCase(source, caseMode)
                TextProcessTab.DEDUPLICATE -> TextProcessEngine.deduplicate(source, options)
                TextProcessTab.SORT -> TextProcessEngine.sortLines(source, sortMode, options)
                TextProcessTab.STATISTICS -> ""
            }
            val nextStatistics = if (tab == TextProcessTab.STATISTICS) {
                TextProcessEngine.statistics(source)
            } else {
                null
            }
            nextOutput to nextStatistics
        }
        if (source == input) {
            output = result.first
            result.second?.let { statistics = it }
        }
        isProcessing = false
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_text_process_name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(TEXT_FILE_MIME_TYPES) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = stringResource(R.string.text_tool_import_file))
                    }
                    IconButton(onClick = { input = readTextFromClipboard(context) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.text_tool_paste))
                    }
                    IconButton(onClick = { input = "" }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 960.dp) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = TextProcessTab.entries.indexOf(tab),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TextProcessTab.entries.forEach { item ->
                        Tab(
                            selected = item == tab,
                            onClick = { tab = item },
                            text = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TextEditorField(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(R.string.text_tool_input),
                        placeholder = stringResource(R.string.text_process_input_placeholder),
                        minLines = 7,
                        maxLines = 12
                    )

                    if (isProcessing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    when (tab) {
                        TextProcessTab.CASE -> CaseControls(
                            selected = caseMode,
                            onSelect = { caseMode = it }
                        )

                        TextProcessTab.DEDUPLICATE -> ProcessOptionControls(
                            trimLines = trimLines,
                            removeEmpty = removeEmpty,
                            ignoreCase = ignoreCase,
                            onTrimLinesChange = { trimLines = it },
                            onRemoveEmptyChange = { removeEmpty = it },
                            onIgnoreCaseChange = { ignoreCase = it }
                        )

                        TextProcessTab.SORT -> SortControls(
                            selected = sortMode,
                            onSelect = { sortMode = it },
                            trimLines = trimLines,
                            removeEmpty = removeEmpty,
                            ignoreCase = ignoreCase,
                            onTrimLinesChange = { trimLines = it },
                            onRemoveEmptyChange = { removeEmpty = it },
                            onIgnoreCaseChange = { ignoreCase = it }
                        )

                        TextProcessTab.STATISTICS -> StatisticsDashboard(statistics)
                    }

                    if (tab != TextProcessTab.STATISTICS) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.text_tool_result),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    copyTextToClipboard(context, output)
                                    scope.launch { snackbarHostState.showSnackbar(copied) }
                                },
                                enabled = output.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
                            }
                        }
                        TextEditorField(
                            value = output,
                            onValueChange = {},
                            label = stringResource(R.string.text_tool_output),
                            placeholder = stringResource(R.string.text_process_output_placeholder),
                            readOnly = true,
                            minLines = 7,
                            maxLines = 12
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaseControls(
    selected: CaseMode,
    onSelect: (CaseMode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CaseMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.labelRes)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessOptionControls(
    trimLines: Boolean,
    removeEmpty: Boolean,
    ignoreCase: Boolean,
    onTrimLinesChange: (Boolean) -> Unit,
    onRemoveEmptyChange: (Boolean) -> Unit,
    onIgnoreCaseChange: (Boolean) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = trimLines,
            onClick = { onTrimLinesChange(!trimLines) },
            label = { Text(stringResource(R.string.text_process_trim_lines)) }
        )
        FilterChip(
            selected = removeEmpty,
            onClick = { onRemoveEmptyChange(!removeEmpty) },
            label = { Text(stringResource(R.string.text_process_remove_empty)) }
        )
        FilterChip(
            selected = ignoreCase,
            onClick = { onIgnoreCaseChange(!ignoreCase) },
            label = { Text(stringResource(R.string.text_process_ignore_case)) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortControls(
    selected: SortMode,
    onSelect: (SortMode) -> Unit,
    trimLines: Boolean,
    removeEmpty: Boolean,
    ignoreCase: Boolean,
    onTrimLinesChange: (Boolean) -> Unit,
    onRemoveEmptyChange: (Boolean) -> Unit,
    onIgnoreCaseChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = { Text(stringResource(mode.labelRes)) }
                )
            }
        }
        ProcessOptionControls(
            trimLines = trimLines,
            removeEmpty = removeEmpty,
            ignoreCase = ignoreCase,
            onTrimLinesChange = onTrimLinesChange,
            onRemoveEmptyChange = onRemoveEmptyChange,
            onIgnoreCaseChange = onIgnoreCaseChange
        )
    }
}

@Composable
private fun StatisticsDashboard(statistics: TextStatistics) {
    val metrics = listOf(
        R.string.text_process_characters to statistics.characters,
        R.string.text_process_characters_no_spaces to statistics.charactersWithoutWhitespace,
        R.string.text_process_letters to statistics.letters,
        R.string.text_process_digits to statistics.digits,
        R.string.text_process_cjk to statistics.cjkCharacters,
        R.string.text_process_whitespace to statistics.whitespaceCharacters,
        R.string.text_process_words to statistics.words,
        R.string.text_process_lines to statistics.lines,
        R.string.text_process_paragraphs to statistics.paragraphs,
        R.string.text_process_utf8_bytes to statistics.utf8Bytes
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMetrics.forEach { (labelRes, value) ->
                    StatisticCard(
                        labelRes = labelRes,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatisticCard(
    @StringRes labelRes: Int,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val CaseMode.labelRes: Int
    get() = when (this) {
        CaseMode.LOWERCASE -> R.string.text_process_lowercase
        CaseMode.UPPERCASE -> R.string.text_process_uppercase
        CaseMode.TITLE -> R.string.text_process_title
        CaseMode.SENTENCE -> R.string.text_process_sentence
        CaseMode.SWAP -> R.string.text_process_swap
    }

private val SortMode.labelRes: Int
    get() = when (this) {
        SortMode.ASCENDING -> R.string.text_process_sort_ascending
        SortMode.DESCENDING -> R.string.text_process_sort_descending
        SortMode.LENGTH_ASCENDING -> R.string.text_process_sort_length_ascending
        SortMode.LENGTH_DESCENDING -> R.string.text_process_sort_length_descending
        SortMode.NATURAL -> R.string.text_process_sort_natural
    }
