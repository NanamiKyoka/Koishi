package com.nanami.koishi.feature.tools.meme_maker

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetPack
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeRenderer
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeStickerAsset
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeTextFont
import com.nanami.koishi.feature.tools.meme_maker.engine.PlacedSticker
import com.nanami.koishi.feature.tools.meme_maker.engine.StickerTransform

enum class MemeColorTarget {
    TEXT,
    STROKE
}

data class MemeMakerUiState(
    val packs: List<MemeAssetPack> = emptyList(),
    val assets: List<MemeStickerAsset> = emptyList(),
    val localStickers: List<MemeLocalSticker> = emptyList(),
    val downloadingPackId: String? = null,
    val downloadProgress: Float = 0f,
    val background: String? = null,
    val stickers: List<PlacedSticker> = emptyList(),
    val selectedStickerId: String? = null,
    val backgroundBitmap: Bitmap? = null,
    val stickerBitmaps: Map<String, Bitmap> = emptyMap(),
    val previewBitmap: Bitmap? = null,
    val canvasWidth: Int = MemeRenderer.DEFAULT_CANVAS_SIZE,
    val canvasHeight: Int = MemeRenderer.DEFAULT_CANVAS_SIZE,
    val textEditorStickerId: String? = null,
    val editorText: String = "",
    val editorFont: MemeTextFont = MemeTextFont.DEFAULT,
    val editorStrokeEnabled: Boolean = true,
    val editorColor: Int = Color.WHITE,
    val editorStrokeColor: Int = Color.BLACK,
    val colorTarget: MemeColorTarget? = null,
    val showAssetPanel: Boolean = false,
    val isProcessing: Boolean = false,
    val progressStage: String = "",
    val userMessage: String? = null,
    val showHelpDialog: Boolean = false
) {
    val selectedSticker: PlacedSticker?
        get() = selectedStickerId?.let { id -> stickers.find { it.id == id } }

    val aspectRatio: Float
        get() = if (canvasHeight <= 0) 1f else canvasWidth.toFloat() / canvasHeight

    val hasContent: Boolean
        get() = stickers.isNotEmpty() || !background.isNullOrBlank()
}

sealed interface MemeMakerUiEvent {
    data class OnBackgroundPicked(val uri: Uri) : MemeMakerUiEvent
    data object OnClearBackground : MemeMakerUiEvent

    data class OnStickerAssetPicked(val asset: MemeStickerAsset) : MemeMakerUiEvent
    data class OnLocalStickerPicked(val uri: Uri) : MemeMakerUiEvent
    data class OnRemoveLocalSticker(val uri: String) : MemeMakerUiEvent
    data object OnAddTextSticker : MemeMakerUiEvent
    data class OnSelectSticker(val stickerId: String?) : MemeMakerUiEvent
    data class OnTransformSticker(val stickerId: String, val transform: StickerTransform) : MemeMakerUiEvent
    data class OnDeleteSticker(val stickerId: String) : MemeMakerUiEvent
    data class OnDuplicateSticker(val stickerId: String) : MemeMakerUiEvent
    data class OnFlipSticker(val stickerId: String) : MemeMakerUiEvent
    data class OnBringStickerToFront(val stickerId: String) : MemeMakerUiEvent
    data class OnSendStickerToBack(val stickerId: String) : MemeMakerUiEvent

    data class OnOpenTextEditor(val stickerId: String) : MemeMakerUiEvent
    data object OnCloseTextEditor : MemeMakerUiEvent
    data class OnEditorTextChanged(val text: String) : MemeMakerUiEvent
    data class OnEditorFontChanged(val font: MemeTextFont) : MemeMakerUiEvent
    data class OnEditorStrokeToggled(val enabled: Boolean) : MemeMakerUiEvent
    data class OnOpenColorPicker(val target: MemeColorTarget) : MemeMakerUiEvent
    data object OnCloseColorPicker : MemeMakerUiEvent
    data class OnColorPicked(val color: Int) : MemeMakerUiEvent

    data class OnDownloadPack(val packId: String) : MemeMakerUiEvent
    data class OnRemovePack(val packId: String) : MemeMakerUiEvent
    data class OnToggleAssetPanel(val show: Boolean) : MemeMakerUiEvent

    data object OnSavePng : MemeMakerUiEvent
    data object OnRequestPreview : MemeMakerUiEvent
    data object OnClosePreview : MemeMakerUiEvent
    data object OnDismissMessage : MemeMakerUiEvent
    data class OnToggleHelpDialog(val show: Boolean) : MemeMakerUiEvent
}
