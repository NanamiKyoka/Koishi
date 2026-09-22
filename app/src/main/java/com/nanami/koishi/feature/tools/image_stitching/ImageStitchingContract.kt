package com.nanami.koishi.feature.tools.image_stitching

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class StitchingMode {
    HORIZONTAL,
    VERTICAL,
    SUBTITLE
}

enum class OutputFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp")
}

enum class StitchBackgroundColor(val color: Color) {
    WHITE(Color.White),
    BLACK(Color.Black),
    TRANSPARENT(Color.Transparent)
}

data class StitchImageItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val filename: String,
    val bitmap: Bitmap,
    val thumbnail: Bitmap,
    val width: Int,
    val height: Int
)

data class StitchingCommonSettings(
    val scaleRatio: Float = 1.0f,
    val gapPx: Int = 0,
    val autoScaleToMin: Boolean = true,
    val backgroundColor: StitchBackgroundColor = StitchBackgroundColor.WHITE,
    val outputFormat: OutputFormat = OutputFormat.PNG
)

data class SubtitleSettings(
    val cropRange: ClosedFloatingPointRange<Float> = 0.72f..0.98f,
    val removeBlackBorders: Boolean = false,
    val isCompressed: Boolean = true,
    val outputFormat: OutputFormat = OutputFormat.JPEG
)

enum class StitchingPageStage {
    IMAGE_LIST,
    SUBTITLE_EDITOR,
    RESULT_PREVIEW
}

data class ImageStitchingUiState(
    val images: List<StitchImageItem> = emptyList(),
    val selectedMode: StitchingMode? = null,
    val showModeDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val commonSettings: StitchingCommonSettings = StitchingCommonSettings(),
    val subtitleSettings: SubtitleSettings = SubtitleSettings(),
    val stage: StitchingPageStage = StitchingPageStage.IMAGE_LIST,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val subtitlePreviewBitmap: Bitmap? = null,
    val resultBitmap: Bitmap? = null,
    val resultDimensions: Pair<Int, Int>? = null,
    val userMessage: String? = null
)

sealed interface ImageStitchingUiEvent {
    data class OnImagesSelected(val uris: List<Uri>) : ImageStitchingUiEvent
    data class OnRemoveImage(val id: String) : ImageStitchingUiEvent
    data object OnClearAllImages : ImageStitchingUiEvent
    data class OnMoveImage(val fromIndex: Int, val toIndex: Int) : ImageStitchingUiEvent
    data class OnShowModeDialog(val show: Boolean) : ImageStitchingUiEvent
    data class OnSelectMode(val mode: StitchingMode) : ImageStitchingUiEvent
    data class OnShowSettingsDialog(val show: Boolean) : ImageStitchingUiEvent
    data class OnUpdateCommonSettings(val settings: StitchingCommonSettings) : ImageStitchingUiEvent
    data class OnUpdateSubtitleSettings(val settings: SubtitleSettings) : ImageStitchingUiEvent
    data object OnStartStitching : ImageStitchingUiEvent
    data object OnSaveResult : ImageStitchingUiEvent
    data object OnBackToImageList : ImageStitchingUiEvent
    data object OnDismissMessage : ImageStitchingUiEvent
}
