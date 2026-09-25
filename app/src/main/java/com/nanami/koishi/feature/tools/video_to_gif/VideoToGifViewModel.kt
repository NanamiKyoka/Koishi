package com.nanami.koishi.feature.tools.video_to_gif

import android.app.Application
import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.video_to_gif.engine.VideoToGifEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class VideoToGifViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoToGifUiState())
    val uiState: StateFlow<VideoToGifUiState> = _uiState.asStateFlow()

    private val engine = VideoToGifEngine(application)
    private var convertJob: Job? = null

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: VideoToGifUiEvent) {
        when (event) {
            is VideoToGifUiEvent.OnVideoSelected -> loadVideo(event.uri)
            is VideoToGifUiEvent.OnClearVideo -> clearVideo()
            is VideoToGifUiEvent.OnTrimRangeChanged -> {
                _uiState.update {
                    it.copy(
                        startTrimMs = event.startMs.coerceAtLeast(0L),
                        endTrimMs = event.endMs.coerceAtMost(it.videoDurationMs)
                    )
                }
            }
            is VideoToGifUiEvent.OnQuickDurationSelected -> {
                val duration = itDuration(event.seconds)
                _uiState.update { state ->
                    state.copy(
                        startTrimMs = 0L,
                        endTrimMs = min(state.videoDurationMs, duration)
                    )
                }
            }
            is VideoToGifUiEvent.OnResolutionChanged -> {
                _uiState.update { it.copy(selectedResolution = event.resolution) }
            }
            is VideoToGifUiEvent.OnFpsChanged -> {
                _uiState.update { it.copy(selectedFps = event.fps) }
            }
            is VideoToGifUiEvent.OnStartConvert -> startConvert()
            is VideoToGifUiEvent.OnCancelConvert -> cancelConvert()
            is VideoToGifUiEvent.OnSaveToGallery -> saveGifToGallery()
            is VideoToGifUiEvent.OnToggleHelpDialog -> {
                _uiState.update { it.copy(isHelpDialogOpen = event.open) }
            }
            is VideoToGifUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun itDuration(seconds: Int): Long = seconds * 1000L

    private fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progressStage = getString(R.string.video_to_gif_reading_info)) }

            val meta = withContext(Dispatchers.IO) {
                parseVideoMetadata(uri)
            }

            if (meta == null || meta.durationMs <= 0) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.video_to_gif_load_failed)
                    )
                }
                return@launch
            }

            val defaultEndTrim = min(meta.durationMs, 5000L)
            _uiState.update {
                it.copy(
                    videoUri = uri,
                    videoName = meta.name,
                    videoDurationMs = meta.durationMs,
                    videoWidth = meta.width,
                    videoHeight = meta.height,
                    startTrimMs = 0L,
                    endTrimMs = defaultEndTrim,
                    resultGifFile = null,
                    resultGifUri = null,
                    resultGifSizeBytes = 0L,
                    isProcessing = false,
                    progressStage = "",
                    progressPercent = 0f
                )
            }
        }
    }

    private fun clearVideo() {
        convertJob?.cancel()
        _uiState.value.resultGifFile?.delete()
        _uiState.value = VideoToGifUiState()
    }

    private fun startConvert() {
        val state = _uiState.value
        val uri = state.videoUri ?: return
        if (!state.canConvert) return

        convertJob?.cancel()
        convertJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressPercent = 0f,
                    progressStage = getString(R.string.video_to_gif_stage_prepare)
                )
            }

            val outputDir = File(getApplication<Application>().cacheDir, "gif_output")
            if (!outputDir.exists()) outputDir.mkdirs()
            val gifFile = File(outputDir, "KOISHI_GIF_${System.currentTimeMillis()}.gif")

            val result = engine.convertVideoToGif(
                sourceUri = uri,
                startTrimMs = state.startTrimMs,
                endTrimMs = state.endTrimMs,
                resolutionScale = state.selectedResolution,
                targetFps = state.selectedFps,
                srcWidth = state.videoWidth,
                srcHeight = state.videoHeight,
                outputGifFile = gifFile,
                onProgress = { percent, stage ->
                    _uiState.update {
                        it.copy(progressPercent = percent, progressStage = stage)
                    }
                }
            )

            result.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progressPercent = 1f,
                            resultGifFile = file,
                            resultGifUri = Uri.fromFile(file),
                            resultGifSizeBytes = file.length(),
                            userMessage = getString(R.string.video_to_gif_convert_success)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            userMessage = getString(R.string.video_to_gif_convert_failed, error.localizedMessage ?: "")
                        )
                    }
                }
            )
        }
    }

    private fun cancelConvert() {
        convertJob?.cancel()
        _uiState.update {
            it.copy(
                isProcessing = false,
                progressStage = "",
                progressPercent = 0f,
                userMessage = getString(R.string.video_to_gif_canceled)
            )
        }
    }

    private fun saveGifToGallery() {
        val file = _uiState.value.resultGifFile ?: return
        if (!file.exists() || _uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = withContext(Dispatchers.IO) {
                exportGifToGallery(file)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = if (success) {
                        getString(R.string.video_to_gif_saved_to_gallery)
                    } else {
                        getString(R.string.video_to_gif_save_failed)
                    }
                )
            }
        }
    }

    private fun exportGifToGallery(gifFile: File): Boolean {
        val context = getApplication<Application>()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "Koishi_${dateStr}.gif"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.VIDEO_TO_GIF)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values) ?: return false

                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(gifFile).use { input ->
                        input.copyTo(out)
                    }
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
                true
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, AlbumFolders.VIDEO_TO_GIF)
                if (!appDir.exists()) appDir.mkdirs()
                val targetFile = File(appDir, filename)

                FileInputStream(gifFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("image/gif"), null)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseVideoMetadata(uri: Uri): VideoMeta? {
        val context = getApplication<Application>()
        val retriever = MediaMetadataRetriever()
        var name = ""

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: ""
                }
            }
        } catch (_: Throwable) {}

        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            val (w, h) = if (rotation == 90 || rotation == 270) {
                Pair(rawHeight, rawWidth)
            } else {
                Pair(rawWidth, rawHeight)
            }

            VideoMeta(name = name, durationMs = durationMs, width = w, height = h)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        convertJob?.cancel()
        _uiState.value.resultGifFile?.delete()
    }

    private data class VideoMeta(
        val name: String,
        val durationMs: Long,
        val width: Int,
        val height: Int
    )
}
