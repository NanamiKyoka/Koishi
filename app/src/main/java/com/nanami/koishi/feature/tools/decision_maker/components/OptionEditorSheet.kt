package com.nanami.koishi.feature.tools.decision_maker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.BottomSheetShape
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionIds
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionWeight
import com.nanami.koishi.feature.tools.decision_maker.engine.sanitized

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionEditorSheet(
    topic: DecisionTopic,
    onDismiss: () -> Unit,
    onSave: (DecisionTopic) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var title by remember(topic.id) { mutableStateOf(topic.title) }
    var options by remember(topic.id) { mutableStateOf(topic.options) }
    var bulkInput by remember(topic.id) { mutableStateOf("") }

    val bulkCandidates = remember(bulkInput) { parseBulkOptions(bulkInput) }
    val validCount = options.count { it.text.isNotBlank() }
    val canSave = title.isNotBlank() && validCount > 0

    fun updateOption(id: String, transform: (DecisionOption) -> DecisionOption) {
        options = options.map { if (it.id == id) transform(it) else it }
    }

    fun appendBulk() {
        val existing = options.map { it.text.trim() }.toMutableSet()
        val appended = bulkCandidates
            .filter { existing.add(it) }
            .map { DecisionOption(id = DecisionIds.newOptionId(), text = it, weight = DecisionWeight.MIN) }
        if (appended.isNotEmpty()) {
            options = options + appended
            bulkInput = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.decision_editor_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.decision_editor_topic_name)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = editorFieldColors()
            )

            Text(
                text = stringResource(R.string.decision_editor_weight_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            options.forEach { option ->
                OptionRow(
                    option = option,
                    onTextChange = { text -> updateOption(option.id) { it.copy(text = text) } },
                    onWeightChange = { weight ->
                        updateOption(option.id) { it.copy(weight = DecisionWeight.clamp(weight)) }
                    },
                    onRemove = { options = options.filterNot { it.id == option.id } }
                )
            }

            OutlinedButton(
                onClick = {
                    options = options + DecisionOption(
                        id = DecisionIds.newOptionId(),
                        text = "",
                        weight = DecisionWeight.MIN
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.decision_editor_add_option))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ToolCardShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.decision_editor_bulk_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = bulkInput,
                        onValueChange = { bulkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.decision_editor_bulk_hint)) },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        colors = editorFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.decision_editor_bulk_parsed,
                                bulkCandidates.size
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { appendBulk() },
                            enabled = bulkCandidates.isNotEmpty(),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.decision_editor_bulk_add))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                Button(
                    onClick = {
                        onSave(topic.copy(title = title, options = options).sanitized())
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.decision_editor_save))
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    option: DecisionOption,
    onTextChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ToolCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = option.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.decision_editor_option_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorFieldColors()
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.decision_editor_remove_option),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.decision_editor_weight, option.weight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = option.weight.toFloat(),
                    onValueChange = { onWeightChange(it.toInt()) },
                    valueRange = DecisionWeight.MIN.toFloat()..DecisionWeight.MAX.toFloat(),
                    steps = DecisionWeight.MAX - DecisionWeight.MIN - 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
)

private fun parseBulkOptions(raw: String): List<String> {
    val seen = LinkedHashSet<String>()
    raw.split('\n').forEach { line ->
        val text = line.trim()
        if (text.isNotEmpty()) seen.add(text)
    }
    return seen.toList()
}
