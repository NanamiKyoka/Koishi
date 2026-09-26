package com.nanami.koishi.feature.tools.meme_maker.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetPack
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeStickerAsset
import com.nanami.koishi.feature.tools.meme_maker.engine.PlacedSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerKind
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerTransform

/**
 * 内嵌在编辑页底部，展开后可连续挑贴纸而不会跳走
 */
@Composable
fun StickerLibraryPanel(
    packs: List<MemeAssetPack>,
    assets: List<MemeStickerAsset>,
    localStickers: List<MemeLocalSticker>,
    downloadingPackId: String?,
    downloadProgress: Float,
    onCollapse: () -> Unit,
    onDownloadPack: (String) -> Unit,
    onRemovePack: (String) -> Unit,
    onPickAsset: (MemeStickerAsset) -> Unit,
    onImportLocal: () -> Unit,
    onPickLocal: (MemeLocalSticker) -> Unit,
    onRemoveLocal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localLabel = stringResource(R.string.meme_local_stickers)
    val tabs = remember(packs, localLabel) {
        packs.map { it.id to it.name } + listOf(LOCAL_TAB_ID to localLabel)
    }
    var selectedTabId by remember { mutableStateOf<String?>(null) }
    val activeTabId = selectedTabId?.takeIf { id -> tabs.any { it.first == id } }
        ?: tabs.firstOrNull()?.first

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.meme_sticker_library),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(R.string.meme_panel_collapse)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (id, label) ->
                    FilterChip(
                        selected = id == activeTabId,
                        onClick = { selectedTabId = id },
                        label = { Text(label) }
                    )
                }
            }

            if (activeTabId == LOCAL_TAB_ID) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onImportLocal) {
                        Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.meme_import_sticker))
                    }
                    Spacer(Modifier.weight(1f))
                    if (localStickers.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.meme_local_stickers_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (localStickers.isEmpty()) {
                    PanelPlaceholder(text = stringResource(R.string.meme_local_stickers_empty))
                } else {
                    StickerGrid(
                        items = localStickers,
                        keyOf = { it.uri },
                        modelOf = { it.uri },
                        labelOf = { it.name },
                        onPick = { onPickLocal(it) },
                        onLongPress = { onRemoveLocal(it.uri) }
                    )
                }
            } else {
                val pack = packs.find { it.id == activeTabId }
                val packAssets = assets.filter { it.packId == activeTabId }
                val isDownloading = downloadingPackId == activeTabId

                when {
                    isDownloading -> {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        PanelPlaceholder(text = stringResource(R.string.meme_pack_downloading))
                    }

                    packAssets.isEmpty() -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = { pack?.let { onDownloadPack(it.id) } }) {
                                Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.meme_pack_download))
                            }
                        }
                        PanelPlaceholder(text = stringResource(R.string.meme_pack_not_downloaded))
                    }

                    else -> {
                        StickerGrid(
                            items = packAssets,
                            keyOf = { it.source.key },
                            modelOf = { it.file },
                            labelOf = { it.displayName },
                            onPick = { onPickAsset(it) }
                        )
                        TextButton(onClick = { pack?.let { onRemovePack(it.id) } }) {
                            Text(stringResource(R.string.meme_pack_remove))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(GRID_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> StickerGrid(
    items: List<T>,
    keyOf: (T) -> String,
    modelOf: (T) -> Any,
    labelOf: (T) -> String,
    onPick: (T) -> Unit,
    onLongPress: ((T) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
        modifier = Modifier.height(GRID_HEIGHT)
    ) {
        items(items, key = { keyOf(it) }) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .combinedClickable(
                            onClick = { onPick(item) },
                            onLongClick = onLongPress?.let { callback -> { callback(item) } }
                        )
                ) {
                    AsyncImage(
                        model = modelOf(item),
                        contentDescription = labelOf(item),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

/**
 * 贴纸调节面板常驻编辑页，未选中贴纸时整体置灰并提示添加，保持编辑态的界面结构不跳变
 */
@Composable
fun StickerTuningPanel(
    sticker: PlacedSticker?,
    onTransformChanged: (StickerTransform) -> Unit,
    onEditText: () -> Unit,
    onFlip: () -> Unit,
    onDuplicate: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = sticker != null
    val transform = sticker?.transform ?: StickerTransform()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StickerAction(Icons.Rounded.VerticalAlignTop, R.string.meme_action_front, onBringToFront, hasSelection)
            StickerAction(Icons.Rounded.VerticalAlignBottom, R.string.meme_action_back, onSendToBack, hasSelection)
            StickerAction(Icons.Rounded.ContentCopy, R.string.meme_action_duplicate, onDuplicate, hasSelection)
            StickerAction(Icons.Rounded.Flip, R.string.meme_action_flip, onFlip, hasSelection)
            StickerAction(
                icon = Icons.Rounded.TextFields,
                labelRes = R.string.meme_action_edit_text,
                onClick = onEditText,
                enabled = sticker?.kind == StickerKind.TEXT
            )
            StickerAction(Icons.Rounded.Delete, R.string.meme_action_delete, onDelete, hasSelection)
        }

        TransformSlider(
            label = stringResource(R.string.meme_action_scale),
            value = transform.scale.coerceIn(MIN_SCALE, MAX_SCALE),
            valueRange = MIN_SCALE..MAX_SCALE,
            enabled = hasSelection,
            onChange = { onTransformChanged(transform.copy(scale = it)) }
        )
        TransformSlider(
            label = stringResource(R.string.meme_action_rotation),
            value = transform.rotation.coerceIn(MIN_ROTATION, MAX_ROTATION),
            valueRange = MIN_ROTATION..MAX_ROTATION,
            enabled = hasSelection,
            onChange = { onTransformChanged(transform.copy(rotation = it)) }
        )
        TransformSlider(
            label = stringResource(R.string.meme_action_alpha),
            value = transform.alpha.coerceIn(0f, 1f),
            valueRange = 0f..1f,
            enabled = hasSelection,
            onChange = { onTransformChanged(transform.copy(alpha = it)) }
        )
    }
}

@Composable
private fun StickerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(imageVector = icon, contentDescription = null)
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            }
        )
    }
}

@Composable
private fun TransformSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
            },
            modifier = Modifier.width(56.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}

private const val LOCAL_TAB_ID = "local"
private val GRID_HEIGHT = 132.dp
private const val MIN_SCALE = 0.03f
private const val MAX_SCALE = 3f
private const val MIN_ROTATION = -180f
private const val MAX_ROTATION = 180f
private const val DISABLED_ALPHA = 0.38f
