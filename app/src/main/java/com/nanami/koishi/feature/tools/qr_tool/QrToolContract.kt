package com.nanami.koishi.feature.tools.qr_tool

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.nanami.koishi.feature.tools.qr_tool.components.QrThemePreset
import com.nanami.koishi.feature.tools.qr_tool.engine.QrDotStyle

enum class ColorPickerTarget {
    DARK,
    LIGHT,
    BACKGROUND
}

data class QrToolUiState(
    val content: String = "https://github.com",
    val darkColor: Color = Color(0xFF3D6B57),
    val lightColor: Color = Color(0xFFEAF2EC),
    val backgroundColor: Color = Color(0xFFFFFFFF),
    val isPickFromBg: Boolean = false,
    val dotStyle: QrDotStyle = QrDotStyle.SQUARE,
    val dotScale: Float = 0.85f,
    val logoUri: Uri? = null,
    val logoBitmap: Bitmap? = null,
    val bgUri: Uri? = null,
    val bgBitmap: Bitmap? = null,
    val bgAlpha: Float = 0.6f,
    val marginDp: Float = 16f,
    val qrBitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val showThemePicker: Boolean = false,
    val activeColorPicker: ColorPickerTarget? = null,
    val userMessage: String? = null
)

sealed interface QrToolUiEvent {
    data class OnContentChange(val content: String) : QrToolUiEvent
    data object OnClearContent : QrToolUiEvent
    data class OnDarkColorChange(val color: Color) : QrToolUiEvent
    data class OnLightColorChange(val color: Color) : QrToolUiEvent
    data class OnBackgroundColorChange(val color: Color) : QrToolUiEvent
    data class OnTogglePickFromBg(val enabled: Boolean) : QrToolUiEvent
    data class OnThemeSelected(val preset: QrThemePreset) : QrToolUiEvent
    data class OnDotStyleChange(val style: QrDotStyle) : QrToolUiEvent
    data class OnDotScaleChange(val scale: Float) : QrToolUiEvent
    data class OnLogoSelected(val uri: Uri) : QrToolUiEvent
    data object OnClearLogo : QrToolUiEvent
    data class OnBgSelected(val uri: Uri) : QrToolUiEvent
    data object OnClearBg : QrToolUiEvent
    data class OnBgAlphaChange(val alpha: Float) : QrToolUiEvent
    data class OnMarginChange(val margin: Float) : QrToolUiEvent
    data object OnOpenThemePicker : QrToolUiEvent
    data object OnDismissThemePicker : QrToolUiEvent
    data class OnOpenColorPicker(val target: ColorPickerTarget) : QrToolUiEvent
    data object OnDismissColorPicker : QrToolUiEvent
    data object OnSaveToGallery : QrToolUiEvent
    data object OnResetDefaults : QrToolUiEvent
    data object OnClearUserMessage : QrToolUiEvent
}
