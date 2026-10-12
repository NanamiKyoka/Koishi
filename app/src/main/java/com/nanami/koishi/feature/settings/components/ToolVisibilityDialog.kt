package com.nanami.koishi.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem

@Composable
fun ToolVisibilityDialog(
    tools: List<ToolItem>,
    hiddenToolIds: Set<String>,
    onToolHiddenChange: (String, Boolean) -> Unit,
    onShowAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val groupedTools = remember(tools) {
        ToolCategory.entries
            .filter { it != ToolCategory.ALL }
            .mapNotNull { category ->
                tools.filter { it.category == category }
                    .takeIf { it.isNotEmpty() }
                    ?.let { category to it }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.VisibilityOff,
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.settings_tool_visibility_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_tool_visibility_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.padding(top = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    groupedTools.forEach { (category, categoryTools) ->
                        item(key = "header_${category.name}") {
                            Text(
                                text = stringResource(category.titleRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(
                            items = categoryTools,
                            key = { it.id }
                        ) { tool ->
                            ToolVisibilityRow(
                                tool = tool,
                                hidden = hiddenToolIds.contains(tool.id),
                                onHiddenChange = { hidden -> onToolHiddenChange(tool.id, hidden) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_tool_visibility_done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onShowAll,
                enabled = hiddenToolIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.settings_tool_visibility_show_all))
            }
        }
    )
}

@Composable
private fun ToolVisibilityRow(
    tool: ToolItem,
    hidden: Boolean,
    onHiddenChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHiddenChange(!hidden) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(tool.nameRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(tool.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = hidden,
            onCheckedChange = onHiddenChange
        )
    }
}
