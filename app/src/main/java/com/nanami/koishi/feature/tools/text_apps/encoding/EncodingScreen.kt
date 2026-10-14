package com.nanami.koishi.feature.tools.text_apps.encoding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

@Composable
fun EncodingRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var scheme by rememberSaveable { mutableStateOf(EncodingScheme.BASE64) }
    var direction by rememberSaveable { mutableStateOf(EncodingDirection.ENCODE) }
    var base64UrlSafe by rememberSaveable { mutableStateOf(false) }
    var includePadding by rememberSaveable { mutableStateOf(true) }
    var urlForm by rememberSaveable { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.text_tool_copied)

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

    LaunchedEffect(input, scheme, direction, base64UrlSafe, includePadding, urlForm) {
        isProcessing = true
        val source = input
        val result = if (source.isEmpty()) {
            Result.success("")
        } else {
            delay(150)
            withContext(Dispatchers.Default) {
                EncodingEngine.transform(
                    input = source,
                    direction = direction,
                    scheme = scheme,
                    options = EncodingOptions(
                        base64UrlSafe = base64UrlSafe,
                        includePadding = includePadding,
                        urlForm = urlForm
                    )
                )
            }
        }
        if (source == input) {
            output = result.getOrDefault("")
            hasError = result.isFailure
        }
        isProcessing = false
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TextToolTopBar(
                titleRes = R.string.tool_text_encoding_name,
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
                        output = ""
                        hasError = false
                    }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.text_tool_clear))
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 840.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = scheme == EncodingScheme.BASE64,
                                onClick = { scheme = EncodingScheme.BASE64 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.text_encoding_base64))
                            }
                            SegmentedButton(
                                selected = scheme == EncodingScheme.URL,
                                onClick = { scheme = EncodingScheme.URL },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.text_encoding_url))
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = direction == EncodingDirection.ENCODE,
                                onClick = { direction = EncodingDirection.ENCODE },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.text_encoding_encode))
                            }
                            SegmentedButton(
                                selected = direction == EncodingDirection.DECODE,
                                onClick = { direction = EncodingDirection.DECODE },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.text_encoding_decode))
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (scheme == EncodingScheme.BASE64) {
                                FilterChip(
                                    selected = base64UrlSafe,
                                    onClick = { base64UrlSafe = !base64UrlSafe },
                                    label = { Text(stringResource(R.string.text_encoding_url_safe)) }
                                )
                                FilterChip(
                                    selected = includePadding,
                                    onClick = { includePadding = !includePadding },
                                    label = { Text(stringResource(R.string.text_encoding_padding)) }
                                )
                            } else {
                                FilterChip(
                                    selected = urlForm,
                                    onClick = { urlForm = !urlForm },
                                    label = { Text(stringResource(R.string.text_encoding_form)) }
                                )
                            }
                        }
                    }
                }

                TextEditorField(
                    value = input,
                    onValueChange = { input = it },
                    label = stringResource(R.string.text_tool_input),
                    placeholder = stringResource(R.string.text_encoding_input_placeholder),
                    minLines = 8,
                    maxLines = 14
                )

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                FilledTonalIconButton(
                    onClick = {
                        direction = if (direction == EncodingDirection.ENCODE) {
                            EncodingDirection.DECODE
                        } else {
                            EncodingDirection.ENCODE
                        }
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = stringResource(R.string.text_tool_reverse_direction))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.text_tool_output),
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
                    label = stringResource(R.string.text_tool_result),
                    placeholder = stringResource(R.string.text_encoding_output_placeholder),
                    readOnly = true,
                    isError = hasError,
                    minLines = 8,
                    maxLines = 14
                )

                if (hasError) {
                    Text(
                        text = stringResource(R.string.text_encoding_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
