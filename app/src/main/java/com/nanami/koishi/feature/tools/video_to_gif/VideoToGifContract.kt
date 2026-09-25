package com.nanami.koishi.feature.tools.video_to_gif

import android.net.Uri
import com.nanami.koishi.R
import java.io.File

enum class ResolutionScale(val labelRes: Int, val maxDimension: Int) {
    ORIGINAL(R.string.video_to_gif_res_original, 0),
    P720(R.string.video_to_gif_res_720p, 720),
    P480(R.string.video_to_gif_res_480p, 480),
    P360(R.string.video_to_gif_res_360p, 360);

    fun calculateTargetDimensions(srcWidth: Int, srcHeight: Int): Pair<Int, Int> {
        if (srcWidth <= 0 || srcHeight <= 0) return Pair(480, 480)
        if (this == ORIGINAL) {
            val maxAllowed = 1280
            val longer = maxOf(srcWidth, srcHeight)
            if (longer > maxAllowed) {
                val scale = maxAllowed.toFloat() / longer
                val w = ((srcWidth * scale).toInt() / 2) * 2
                val h = ((srcHeight * scale).toInt() / 2) * 2
                return Pair(maxOf(2, w), maxOf(2, h))
            }
            return Pair(maxOf(2, (srcWidth / 2) * 2), maxOf(2, (srcHeight / 2) * 2))
        }
        val isPortrait = srcHeight > srcWidth
        val (targetW, targetH) = if (isPortrait) {
            val scale = maxDimension.toFloat() / srcWidth
            Pair(maxDimension, (srcHeight * scale).toInt())
        } else {
            val scale = maxDimension.toFloat() / srcHeight
            Pair((srcWidth * scale).toInt(), maxDimension)
        }
        val w = (targetW / 2) * 2
        val h = (targetH / 2) * 2
        return Pair(maxOf(2, w), maxOf(2, h))
    }
}

data class VideoToGifUiState(
    val videoUri: Uri? = null,
    val videoName: String = "",
    val videoDurationMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 0L,
    val selectedResolution: ResolutionScale = ResolutionScale.P480,
    val selectedFps: Int = 15,
    val isProcessing: Boolean = false,
    val progressStage: String = "",
    val progressPercent: Float = 0f,
    val resultGifFile: File? = null,
    val resultGifUri: Uri? = null,
    val resultGifSizeBytes: Long = 0L,
    val isSaving: Boolean = false,
    val userMessage: String? = null,
    val isHelpDialogOpen: Boolean = false
) {
    val hasVideo: Boolean
        get() = videoUri != null && videoDurationMs > 0

    val trimDurationMs: Long
        get() = maxOf(0L, endTrimMs - startTrimMs)

    val estimatedFrameCount: Int
        get() = ((trimDurationMs / 1000f) * selectedFps).toInt().coerceAtLeast(1)

    val canConvert: Boolean
        get() = hasVideo && trimDurationMs >= 200 && !isProcessing
}

sealed interface VideoToGifUiEvent {
    data class OnVideoSelected(val uri: Uri) : VideoToGifUiEvent
    data object OnClearVideo : VideoToGifUiEvent
    data class OnTrimRangeChanged(val startMs: Long, val endMs: Long) : VideoToGifUiEvent
    data class OnQuickDurationSelected(val seconds: Int) : VideoToGifUiEvent
    data class OnResolutionChanged(val resolution: ResolutionScale) : VideoToGifUiEvent
    data class OnFpsChanged(val fps: Int) : VideoToGifUiEvent
    data object OnStartConvert : VideoToGifUiEvent
    data object OnCancelConvert : VideoToGifUiEvent
    data object OnSaveToGallery : VideoToGifUiEvent
    data class OnToggleHelpDialog(val open: Boolean) : VideoToGifUiEvent
    data object OnDismissMessage : VideoToGifUiEvent
}
