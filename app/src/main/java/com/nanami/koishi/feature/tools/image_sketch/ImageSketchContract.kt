package com.nanami.koishi.feature.tools.image_sketch

import android.graphics.Bitmap
import android.net.Uri
import com.nanami.koishi.feature.tools.image_sketch.engine.ImageSketchParams

/**
 * 图片素描界面状态
 */
data class ImageSketchUiState(
    val sourceUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val previewSourceBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val params: ImageSketchParams = ImageSketchParams(),
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val userMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
) {
    val hasImage: Boolean
        get() = sourceBitmap != null

    val effectiveRadius: Int
        get() = params.effectiveRadius

    val canSave: Boolean
        get() = previewBitmap != null && !isProcessing && !isSaving
}

/**
 * 图片素描 UI 交互事件
 */
sealed interface ImageSketchUiEvent {
    data class OnImageSelected(val uri: Uri) : ImageSketchUiEvent
    data object OnClearImage : ImageSketchUiEvent
    data class OnRadiusChanged(val radius: Int) : ImageSketchUiEvent
    data object OnResetParams : ImageSketchUiEvent
    data object OnSaveResult : ImageSketchUiEvent
    data class OnToggleHelpDialog(val open: Boolean) : ImageSketchUiEvent
    data object OnDismissMessage : ImageSketchUiEvent
}
