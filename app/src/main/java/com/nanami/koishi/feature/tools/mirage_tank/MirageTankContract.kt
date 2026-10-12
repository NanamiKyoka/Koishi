package com.nanami.koishi.feature.tools.mirage_tank

import android.graphics.Bitmap
import android.net.Uri
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankMode
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankParams

/**
 * 选取的单张图片数据项
 */
data class MirageTankImageItem(
    val uri: Uri,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)

/**
 * 预览背景色模式
 */
enum class PreviewBackgroundMode {
    /**
     * 白底模式（即时查看表图效果）
     */
    WHITE,

    /**
     * 黑底模式（即时查看里图效果）
     */
    BLACK,

    /**
     * 实时无极切换模式（通过滑块平滑调节背景明暗）
     */
    CUSTOM
}

/**
 * 幻影坦克页面分区
 */
enum class MirageTankPanel {
    CREATE,
    REVEAL
}

/**
 * 幻影坦克 UI 状态
 */
data class MirageTankUiState(
    val panel: MirageTankPanel = MirageTankPanel.CREATE,
    val frontImage: MirageTankImageItem? = null,
    val backImage: MirageTankImageItem? = null,
    val resultBitmap: Bitmap? = null,
    val sourceImage: MirageTankImageItem? = null,
    val revealedFront: Bitmap? = null,
    val revealedBack: Bitmap? = null,
    val params: MirageTankParams = MirageTankParams(),
    val previewBackgroundMode: PreviewBackgroundMode = PreviewBackgroundMode.WHITE,
    val customBackgroundRatio: Float = 0.5f,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val userMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
) {
    /**
     * 当前用于预览渲染的有效背景亮度比例 (0.0f = 纯黑, 1.0f = 纯白)
     */
    val currentBackgroundRatio: Float
        get() = when (previewBackgroundMode) {
            PreviewBackgroundMode.WHITE -> 1.0f
            PreviewBackgroundMode.BLACK -> 0.0f
            PreviewBackgroundMode.CUSTOM -> customBackgroundRatio
        }

    /**
     * 是否已同时选取表图与里图
     */
    val hasBothImages: Boolean
        get() = frontImage != null && backImage != null

    /**
     * 是否可以执行保存操作
     */
    val canSave: Boolean
        get() = resultBitmap != null && !isProcessing && !isSaving

    /**
     * 是否可以执行还原操作
     */
    val canReveal: Boolean
        get() = sourceImage != null && !isProcessing

    /**
     * 是否已还原出表图或里图
     */
    val hasRevealed: Boolean
        get() = revealedFront != null || revealedBack != null
}

/**
 * 幻影坦克 UI 事件交互接口
 */
sealed interface MirageTankUiEvent {
    data class OnPanelChanged(val panel: MirageTankPanel) : MirageTankUiEvent
    data class OnFrontImageSelected(val uri: Uri) : MirageTankUiEvent
    data class OnBackImageSelected(val uri: Uri) : MirageTankUiEvent
    data object OnClearFrontImage : MirageTankUiEvent
    data object OnClearBackImage : MirageTankUiEvent
    data object OnSwapImages : MirageTankUiEvent

    data class OnModeChanged(val mode: MirageTankMode) : MirageTankUiEvent
    data class OnFrontLightnessChanged(val value: Float) : MirageTankUiEvent
    data class OnFrontContrastChanged(val value: Float) : MirageTankUiEvent
    data class OnBackLightnessChanged(val value: Float) : MirageTankUiEvent
    data class OnBackContrastChanged(val value: Float) : MirageTankUiEvent
    data class OnCheckerboardToggled(val enabled: Boolean) : MirageTankUiEvent
    data class OnCheckerboardStrengthChanged(val value: Float) : MirageTankUiEvent
    data object OnResetParams : MirageTankUiEvent

    data class OnPreviewBackgroundModeChanged(val mode: PreviewBackgroundMode) : MirageTankUiEvent
    data class OnCustomBackgroundRatioChanged(val ratio: Float) : MirageTankUiEvent

    data class OnSourceImageSelected(val uri: Uri) : MirageTankUiEvent
    data object OnClearSourceImage : MirageTankUiEvent
    data object OnSaveRevealedFront : MirageTankUiEvent
    data object OnSaveRevealedBack : MirageTankUiEvent

    data object OnGenerateTank : MirageTankUiEvent
    data object OnSaveTank : MirageTankUiEvent
    data class OnToggleHelpDialog(val open: Boolean) : MirageTankUiEvent
    data object OnDismissMessage : MirageTankUiEvent
}
