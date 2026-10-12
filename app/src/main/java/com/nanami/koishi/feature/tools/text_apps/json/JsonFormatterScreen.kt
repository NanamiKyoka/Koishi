package com.nanami.koishi.feature.tools.text_apps.json

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.nanami.koishi.feature.tools.text_apps.common.TextEditorField
import com.nanami.koishi.feature.tools.text_apps.common.TextToolTopBar
import com.nanami.koishi.feature.tools.text_apps.common.copyTextToClipboard
import com.nanami.koishi.feature.tools.text_apps.common.readTextFromClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JsonFormatterRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var errorDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var indentSize by rememberSaveable { mutableIntStateOf(2) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val invalidJson = stringResource(R.string.text_tool_invalid_json)
    val validJson = stringResource(R.string.text_tool_valid_json)
    val copied = stringResource(R.string.text_tool_copied)

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun applyFormat(minify: Boolean) {
        val result = if (minify) JsonFormatterEngine.minify(input) else JsonFormatterEngine.format(input, indentSize)
        output = result.getOrDefault("")
        errorDetail = result.exceptionOrNull()?.message
    }

    fun validate() {
        val result = JsonFormatterEngine.minify(input)
        errorDetail = result.exceptionOrNull()?.message
        showMessage(if (result.isSuccess) validJson else invalidJson)
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
                    IconButton(onClick = { input = readTextFromClipboard(context) }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.text_tool_paste))
                    }
                    IconButton(onClick = {
                        input = ""
                        output = ""
                        errorDetail = null
                    }) {
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
                        Button(onClick = { applyFormat(false) }) {
                            Text(stringResource(R.string.text_tool_format))
                        }
                        OutlinedButton(onClick = { applyFormat(true) }) {
                            Text(stringResource(R.string.text_tool_minify))
                        }
                        TextButton(onClick = ::validate) {
                            Text(stringResource(R.string.text_tool_validate))
                        }
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = indentSize == 2,
                            onClick = { indentSize = 2 },
                            label = { Text(stringResource(R.string.text_tool_indent_spaces, 2)) }
                        )
                        FilterChip(
                            selected = indentSize == 4,
                            onClick = { indentSize = 4 },
                            label = { Text(stringResource(R.string.text_tool_indent_spaces, 4)) }
                        )
                    }
                }

                if (layoutMode == WindowLayoutMode.EXPANDED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        JsonInputField(
                            value = input,
                            onValueChange = {
                                input = it
                                errorDetail = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        JsonOutputField(
                            value = output,
                            errorDetail = errorDetail,
                            onCopy = {
                                copyTextToClipboard(context, output)
                                showMessage(copied)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    JsonInputField(
                        value = input,
                        onValueChange = {
                            input = it
                            errorDetail = null
                        }
                    )
                    JsonOutputField(
                        value = output,
                        errorDetail = errorDetail,
                        onCopy = {
                            copyTextToClipboard(context, output)
                            showMessage(copied)
                        }
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
    errorDetail: String?,
    onCopy: () -> Unit,
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
            IconButton(onClick = onCopy, enabled = value.isNotEmpty()) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.text_tool_copy))
            }
        }
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
