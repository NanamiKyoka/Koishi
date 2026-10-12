package com.nanami.koishi.feature.tools.prism_tank

import android.graphics.Bitmap
import android.net.Uri
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankFill
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankParams
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankRevealConfig

data class PrismTankImageItem(
    val uri: Uri,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)

enum class PrismTankPanel {
    CREATE,
    REVEAL
}

data class PrismTankUiState(
    val panel: PrismTankPanel = PrismTankPanel.CREATE,
    val innerImage: PrismTankImageItem? = null,
    val coverImage: PrismTankImageItem? = null,
    val resultBitmap: Bitmap? = null,
    val params: PrismTankParams = PrismTankParams(),
    val exposureGain: Float = MIN_EXPOSURE_GAIN,
    val exposurePreview: Bitmap? = null,
    val sourceImage: PrismTankImageItem? = null,
    val revealConfig: PrismTankRevealConfig = PrismTankRevealConfig(),
    val revealedInner: Bitmap? = null,
    val revealedCover: Bitmap? = null,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val userMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
) {
    val hasBothImages: Boolean
        get() = innerImage != null && coverImage != null

    val canSave: Boolean
        get() = resultBitmap != null && !isProcessing && !isSaving


    companion object {
        const val MIN_EXPOSURE_GAIN = 1f
        const val MAX_EXPOSURE_GAIN = 20f
    }
}

sealed interface PrismTankUiEvent {
    data class OnPanelChanged(val panel: PrismTankPanel) : PrismTankUiEvent

    data class OnInnerImageSelected(val uri: Uri) : PrismTankUiEvent
    data class OnCoverImageSelected(val uri: Uri) : PrismTankUiEvent
    data object OnClearInnerImage : PrismTankUiEvent
    data object OnClearCoverImage : PrismTankUiEvent
    data object OnSwapImages : PrismTankUiEvent

    data class OnInnerThresholdChanged(val value: Int) : PrismTankUiEvent
    data class OnCoverThresholdChanged(val value: Int) : PrismTankUiEvent
    data class OnGapChanged(val value: Int) : PrismTankUiEvent
    data class OnSlopeChanged(val value: Int) : PrismTankUiEvent
    data class OnInnerGrayToggled(val enabled: Boolean) : PrismTankUiEvent
    data class OnCoverGrayToggled(val enabled: Boolean) : PrismTankUiEvent
    data class OnReverseToggled(val enabled: Boolean) : PrismTankUiEvent
    data object OnResetParams : PrismTankUiEvent

    data class OnExposureGainChanged(val value: Float) : PrismTankUiEvent

    data class OnSourceImageSelected(val uri: Uri) : PrismTankUiEvent
    data object OnClearSourceImage : PrismTankUiEvent
    data class OnRevealLowerChanged(val value: Int) : PrismTankUiEvent
    data class OnRevealUpperChanged(val value: Int) : PrismTankUiEvent
    data class OnRevealContrastChanged(val value: Float) : PrismTankUiEvent
    data class OnRevealFillChanged(val fill: PrismTankFill) : PrismTankUiEvent
    data object OnResetRevealConfig : PrismTankUiEvent
    data object OnReveal : PrismTankUiEvent
    data object OnSaveRevealedInner : PrismTankUiEvent
    data object OnSaveRevealedCover : PrismTankUiEvent

    data object OnGenerateTank : PrismTankUiEvent
    data object OnSaveTank : PrismTankUiEvent
    data class OnToggleHelpDialog(val open: Boolean) : PrismTankUiEvent
    data object OnDismissMessage : PrismTankUiEvent
}