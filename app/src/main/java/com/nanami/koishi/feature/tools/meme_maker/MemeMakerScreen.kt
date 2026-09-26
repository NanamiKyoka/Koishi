package com.nanami.koishi.feature.tools.meme_maker

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.component.ClassicColorPickerDialog
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.meme_maker.components.StickerCanvas
import com.nanami.koishi.feature.tools.meme_maker.components.StickerLibraryPanel
import com.nanami.koishi.feature.tools.meme_maker.components.StickerTuningPanel
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeTextFont

@Composable
fun MemeMakerRoute(
    viewModel: MemeMakerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MemeMakerUiEvent.OnDismissMessage)
        }
    }

    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onEvent(MemeMakerUiEvent.OnBackgroundPicked(it)) }
    }

    val localStickerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onEvent(MemeMakerUiEvent.OnLocalStickerPicked(it)) }
    }

    MemeMakerScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onPickBackground = {
            backgroundPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPickLocalSticker = {
            localStickerPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeMakerScreen(
    state: MemeMakerUiState,
    onEvent: (MemeMakerUiEvent) -> Unit,
    onBack: () -> Unit,
    onPickBackground: () -> Unit,
    onPickLocalSticker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSticker = state.selectedSticker

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meme_maker_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(MemeMakerUiEvent.OnToggleHelpDialog(true)) }) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
                    }
                    IconButton(onClick = { onEvent(MemeMakerUiEvent.OnSavePng) }) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                StickerCanvas(
                    background = state.backgroundBitmap,
                    stickers = state.stickers,
                    stickerBitmaps = state.stickerBitmaps,
                    canvasWidth = state.canvasWidth,
                    canvasHeight = state.canvasHeight,
                    selectedStickerId = state.selectedStickerId,
                    onSelectSticker = { onEvent(MemeMakerUiEvent.OnSelectSticker(it)) },
                    onTransformSticker = { id, transform ->
                        onEvent(MemeMakerUiEvent.OnTransformSticker(id, transform))
                    },
                    onRequestPreview = { onEvent(MemeMakerUiEvent.OnRequestPreview) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (state.showAssetPanel) {
                StickerLibraryPanel(
                    packs = state.packs,
                    assets = state.assets,
                    localStickers = state.localStickers,
                    downloadingPackId = state.downloadingPackId,
                    downloadProgress = state.downloadProgress,
                    onCollapse = { onEvent(MemeMakerUiEvent.OnToggleAssetPanel(false)) },
                    onDownloadPack = { onEvent(MemeMakerUiEvent.OnDownloadPack(it)) },
                    onRemovePack = { onEvent(MemeMakerUiEvent.OnRemovePack(it)) },
                    onPickAsset = { onEvent(MemeMakerUiEvent.OnStickerAssetPicked(it)) },
                    onImportLocal = onPickLocalSticker,
                    onPickLocal = { onEvent(MemeMakerUiEvent.OnLocalStickerPicked(Uri.parse(it.uri))) },
                    onRemoveLocal = { onEvent(MemeMakerUiEvent.OnRemoveLocalSticker(it)) }
                )
            } else {
                StickerTuningPanel(
                    sticker = selectedSticker,
                    onTransformChanged = { transform ->
                        selectedSticker?.let {
                            onEvent(MemeMakerUiEvent.OnTransformSticker(it.id, transform))
                        }
                    },
                    onEditText = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnOpenTextEditor(it.id)) } },
                    onFlip = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnFlipSticker(it.id)) } },
                    onDuplicate = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnDuplicateSticker(it.id)) } },
                    onBringToFront = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnBringStickerToFront(it.id)) } },
                    onSendToBack = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnSendStickerToBack(it.id)) } },
                    onDelete = { selectedSticker?.let { onEvent(MemeMakerUiEvent.OnDeleteSticker(it.id)) } }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolBarButton(
                    icon = Icons.Rounded.Layers,
                    labelRes = R.string.meme_sticker_library,
                    emphasized = state.showAssetPanel,
                    onClick = { onEvent(MemeMakerUiEvent.OnToggleAssetPanel(!state.showAssetPanel)) },
                    modifier = Modifier.weight(1f)
                )
                ToolBarButton(
                    icon = Icons.Rounded.TextFields,
                    labelRes = R.string.meme_add_text_sticker,
                    onClick = { onEvent(MemeMakerUiEvent.OnAddTextSticker) },
                    modifier = Modifier.weight(1f)
                )
                ToolBarButton(
                    icon = Icons.Rounded.Image,
                    labelRes = R.string.meme_pick_background,
                    onClick = onPickBackground,
                    onLongClick = if (state.background != null) {
                        { onEvent(MemeMakerUiEvent.OnClearBackground) }
                    } else {
                        null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val editingStickerId = state.textEditorStickerId
    if (editingStickerId != null) {
        TextStickerEditorDialog(
            text = state.editorText,
            font = state.editorFont,
            strokeEnabled = state.editorStrokeEnabled,
            color = state.editorColor,
            strokeColor = state.editorStrokeColor,
            onTextChanged = { onEvent(MemeMakerUiEvent.OnEditorTextChanged(it)) },
            onFontChanged = { onEvent(MemeMakerUiEvent.OnEditorFontChanged(it)) },
            onStrokeToggled = { onEvent(MemeMakerUiEvent.OnEditorStrokeToggled(it)) },
            onPickColor = { onEvent(MemeMakerUiEvent.OnOpenColorPicker(it)) },
            onDeleteSticker = { onEvent(MemeMakerUiEvent.OnDeleteSticker(editingStickerId)) },
            onDismiss = { onEvent(MemeMakerUiEvent.OnCloseTextEditor) }
        )
    }

    state.colorTarget?.let { target ->
        ClassicColorPickerDialog(
            title = stringResource(
                if (target == MemeColorTarget.TEXT) R.string.meme_text_color else R.string.meme_text_stroke_color
            ),
            initialColor = Color(
                if (target == MemeColorTarget.TEXT) state.editorColor else state.editorStrokeColor
            ),
            onDismissRequest = { onEvent(MemeMakerUiEvent.OnCloseColorPicker) },
            onColorSelected = { onEvent(MemeMakerUiEvent.OnColorPicked(it.toArgb())) }
        )
    }

    if (state.showHelpDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(MemeMakerUiEvent.OnToggleHelpDialog(false)) },
            title = { Text(stringResource(R.string.meme_help_title)) },
            text = { Text(stringResource(R.string.meme_help_content)) },
            confirmButton = {
                TextButton(onClick = { onEvent(MemeMakerUiEvent.OnToggleHelpDialog(false)) }) {
                    Text(stringResource(R.string.meme_confirm))
                }
            }
        )
    }

    state.previewBitmap?.let { bitmap ->
        ImagePreviewDialog(
            bitmap = bitmap,
            title = stringResource(R.string.meme_preview_title),
            albumFolder = AlbumFolders.MEME_MAKER,
            onDismissRequest = { onEvent(MemeMakerUiEvent.OnClosePreview) }
        )
    }

    if (state.isProcessing && state.progressStage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(state.progressStage) },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolBarButton(
    icon: ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = modifier
            .height(58.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TextStickerEditorDialog(
    text: String,
    font: MemeTextFont,
    strokeEnabled: Boolean,
    color: Int,
    strokeColor: Int,
    onTextChanged: (String) -> Unit,
    onFontChanged: (MemeTextFont) -> Unit,
    onStrokeToggled: (Boolean) -> Unit,
    onPickColor: (MemeColorTarget) -> Unit,
    onDeleteSticker: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meme_text_editor_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    label = { Text(stringResource(R.string.meme_text_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.meme_text_font),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MemeTextFont.entries.forEach { item ->
                        FilterChip(
                            selected = font == item,
                            onClick = { onFontChanged(item) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.meme_text_stroke),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = strokeEnabled, onCheckedChange = onStrokeToggled)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorButton(
                        label = stringResource(R.string.meme_text_color),
                        color = color,
                        onClick = { onPickColor(MemeColorTarget.TEXT) },
                        modifier = Modifier.weight(1f)
                    )
                    ColorButton(
                        label = stringResource(R.string.meme_text_stroke_color),
                        color = strokeColor,
                        onClick = { onPickColor(MemeColorTarget.STROKE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meme_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeleteSticker) {
                Text(
                    text = stringResource(R.string.meme_text_delete_sticker),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
private fun ColorButton(
    label: String,
    color: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(onClick = onClick, shape = PillShape, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Color(color), PillShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}
