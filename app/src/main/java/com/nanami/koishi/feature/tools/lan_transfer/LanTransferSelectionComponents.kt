package com.nanami.koishi.feature.tools.lan_transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.lan_transfer.engine.transfer.PickedFile

@Composable
internal fun SendSelectionOptions(
    onPickFiles: () -> Unit,
    onPickMedia: () -> Unit,
    onPickText: () -> Unit,
    onPickFolder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.lan_transfer_selection_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelectionOptionButton(
                    icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                    label = stringResource(R.string.lan_transfer_pick_file),
                    onClick = onPickFiles
                )
            }
            item {
                SelectionOptionButton(
                    icon = Icons.Rounded.Image,
                    label = stringResource(R.string.lan_transfer_pick_media),
                    onClick = onPickMedia
                )
            }
            item {
                SelectionOptionButton(
                    icon = Icons.AutoMirrored.Rounded.TextSnippet,
                    label = stringResource(R.string.lan_transfer_pick_text),
                    onClick = onPickText
                )
            }
            item {
                SelectionOptionButton(
                    icon = Icons.Rounded.Folder,
                    label = stringResource(R.string.lan_transfer_pick_folder),
                    onClick = onPickFolder
                )
            }
        }
    }
}

@Composable
private fun SelectionOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SelectedItemsSummaryCard(
    files: List<PickedFile>,
    onClear: () -> Unit,
    onEdit: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.lan_transfer_selection_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.lan_transfer_clear_files)
                    )
                }
            }
            Text(
                text = stringResource(R.string.lan_transfer_selection_count, files.size),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.lan_transfer_selection_size,
                    formatBytes(files.sumOf { it.size })
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    SendItemThumbnail(
                        file = file,
                        size = 64.dp
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.lan_transfer_edit_items))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.lan_transfer_add_items))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemsBottomSheet(
    onDismiss: () -> Unit,
    onPickFiles: () -> Unit,
    onPickMedia: () -> Unit,
    onPickText: () -> Unit,
    onPickFolder: () -> Unit
) {
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.lan_transfer_add_items_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.lan_transfer_add_items_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 3
            ) {
                AddItemButton(
                    icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                    label = stringResource(R.string.lan_transfer_pick_file),
                    onClick = onPickFiles
                )
                AddItemButton(
                    icon = Icons.Rounded.Image,
                    label = stringResource(R.string.lan_transfer_pick_media),
                    onClick = onPickMedia
                )
                AddItemButton(
                    icon = Icons.AutoMirrored.Rounded.TextSnippet,
                    label = stringResource(R.string.lan_transfer_pick_text),
                    onClick = onPickText
                )
                AddItemButton(
                    icon = Icons.Rounded.Folder,
                    label = stringResource(R.string.lan_transfer_pick_folder),
                    onClick = onPickFolder
                )
            }
        }
    }
}

@Composable
private fun AddItemButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(92.dp)
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun SendItemThumbnail(
    file: PickedFile,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
    ) {
        when {
            file.mimeType.startsWith("image/") -> AsyncImage(
                model = file.uri,
                contentDescription = file.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            file.mimeType.startsWith("video/") -> ThumbnailIcon(Icons.Rounded.Movie)

            file.mimeType.startsWith("text/") && file.previewText != null ->
                ThumbnailIcon(Icons.AutoMirrored.Rounded.TextSnippet)

            else -> ThumbnailIcon(Icons.AutoMirrored.Rounded.InsertDriveFile)
        }
    }
}

@Composable
private fun ThumbnailIcon(icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}