package com.nanami.koishi.feature.tools.watermark

import android.graphics.Bitmap
import android.net.Uri
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkConfig
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkFont
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkType

/**
 * 水印图界面状态
 */
data class WatermarkUiState(
    val backgroundUri: Uri? = null,
    val backgroundBitmap: Bitmap? = null,
    val watermarkImageUri: Uri? = null,
    val watermarkImageBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val config: WatermarkConfig = WatermarkConfig(),
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val showColorPicker: Boolean = false,
    val userMessage: String? = null
)

/**
 * 水印图 UI 交互事件
 */
sealed interface WatermarkUiEvent {
    data class OnBackgroundSelected(val uri: Uri) : WatermarkUiEvent
    data object OnClearBackground : WatermarkUiEvent
    data class OnWatermarkImageSelected(val uri: Uri) : WatermarkUiEvent
    data object OnClearWatermarkImage : WatermarkUiEvent
    data class OnModeChanged(val mode: WatermarkType) : WatermarkUiEvent
    data class OnTextChanged(val text: String) : WatermarkUiEvent
    data class OnTextColorChanged(val color: Int) : WatermarkUiEvent
    data class OnTextSizeChanged(val size: Float) : WatermarkUiEvent
    data class OnFontChanged(val font: WatermarkFont) : WatermarkUiEvent
    data class OnAlphaChanged(val alpha: Float) : WatermarkUiEvent
    data class OnRotationChanged(val rotation: Float) : WatermarkUiEvent
    data class OnImageScaleChanged(val scale: Float) : WatermarkUiEvent
    data class OnHorizontalSpacingChanged(val spacing: Float) : WatermarkUiEvent
    data class OnVerticalSpacingChanged(val spacing: Float) : WatermarkUiEvent
    data class OnToggleColorPicker(val show: Boolean) : WatermarkUiEvent
    data object OnResetConfig : WatermarkUiEvent
    data object OnSaveResult : WatermarkUiEvent
    data object OnDismissMessage : WatermarkUiEvent
}
