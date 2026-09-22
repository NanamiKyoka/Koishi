package com.nanami.koishi.feature.tools.image_stitching

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.feature.tools.image_stitching.engine.ImageStitchingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class ImageStitchingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageStitchingUiState())
    val uiState: StateFlow<ImageStitchingUiState> = _uiState.asStateFlow()

    private var subtitlePreviewJob: Job? = null

    fun onEvent(event: ImageStitchingUiEvent) {
        when (event) {
            is ImageStitchingUiEvent.OnImagesSelected -> loadSelectedImages(event.uris)
            is ImageStitchingUiEvent.OnRemoveImage -> removeImage(event.id)
            is ImageStitchingUiEvent.OnClearAllImages -> clearAllImages()
            is ImageStitchingUiEvent.OnMoveImage -> moveImage(event.fromIndex, event.toIndex)
            is ImageStitchingUiEvent.OnShowModeDialog -> _uiState.update { it.copy(showModeDialog = event.show) }
            is ImageStitchingUiEvent.OnSelectMode -> selectMode(event.mode)
            is ImageStitchingUiEvent.OnShowSettingsDialog -> _uiState.update { it.copy(showSettingsDialog = event.show) }
            is ImageStitchingUiEvent.OnUpdateCommonSettings -> _uiState.update { it.copy(commonSettings = event.settings) }
            is ImageStitchingUiEvent.OnUpdateSubtitleSettings -> updateSubtitleSettings(event.settings)
            is ImageStitchingUiEvent.OnStartStitching -> startCommonStitching()
            is ImageStitchingUiEvent.OnSaveResult -> saveCurrentResult()
            is ImageStitchingUiEvent.OnBackToImageList -> _uiState.update { it.copy(stage = StitchingPageStage.IMAGE_LIST) }
            is ImageStitchingUiEvent.OnDismissMessage -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun loadSelectedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true) }
            val loadedItems = mutableListOf<StitchImageItem>()

            for (uri in uris) {
                try {
                    var displayName = "image_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) displayName = cursor.getString(idx) ?: displayName
                        }
                    }

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }

                    val rawW = options.outWidth
                    val rawH = options.outHeight
                    if (rawW <= 0 || rawH <= 0) continue

                    var sampleSize = 1
                    val maxDim = max(rawW, rawH)
                    while (maxDim / sampleSize > 2560) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val fullBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: continue

                    val thumbScale = 240f / max(fullBitmap.width, fullBitmap.height)
                    val thumbW = max(1, (fullBitmap.width * thumbScale).toInt())
                    val thumbH = max(1, (fullBitmap.height * thumbScale).toInt())
                    val thumbnail = Bitmap.createScaledBitmap(fullBitmap, thumbW, thumbH, true)

                    loadedItems.add(
                        StitchImageItem(
                            uri = uri,
                            filename = displayName,
                            bitmap = fullBitmap,
                            thumbnail = thumbnail,
                            width = rawW,
                            height = rawH
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        images = state.images + loadedItems,
                        isProcessing = false
                    )
                }
            }
        }
    }

    private fun removeImage(id: String) {
        _uiState.update { state ->
            val updated = state.images.filterNot { it.id == id }
            state.copy(images = updated)
        }
    }

    private fun clearAllImages() {
        _uiState.update { state ->
            state.copy(images = emptyList())
        }
    }

    private fun moveImage(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.images.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                state.copy(images = list)
            } else {
                state
            }
        }
    }

    private fun selectMode(mode: StitchingMode) {
        _uiState.update { it.copy(selectedMode = mode, showModeDialog = false) }
        when (mode) {
            StitchingMode.SUBTITLE -> {
                _uiState.update { it.copy(stage = StitchingPageStage.SUBTITLE_EDITOR) }
                scheduleSubtitlePreview()
            }
            StitchingMode.HORIZONTAL, StitchingMode.VERTICAL -> {
                _uiState.update { it.copy(showSettingsDialog = true) }
            }
        }
    }

    private fun updateSubtitleSettings(settings: SubtitleSettings) {
        _uiState.update { it.copy(subtitleSettings = settings) }
        scheduleSubtitlePreview()
    }

    private fun scheduleSubtitlePreview() {
        subtitlePreviewJob?.cancel()
        subtitlePreviewJob = viewModelScope.launch(Dispatchers.Default) {
            delay(120)
            val images = _uiState.value.images.map { it.bitmap }
            if (images.isEmpty()) return@launch

            val preview = try {
                ImageStitchingEngine.stitchSubtitles(
                    bitmaps = images,
                    settings = _uiState.value.subtitleSettings,
                    maxPreviewDimension = 1280
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(subtitlePreviewBitmap = preview) }
            }
        }
    }

    private fun startCommonStitching() {
        val state = _uiState.value
        val bitmaps = state.images.map { it.bitmap }
        if (bitmaps.size < 2) {
            _uiState.update { it.copy(userMessage = "请至少选择 2 张图片进行拼接") }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, showSettingsDialog = false) }

            try {
                val result = when (state.selectedMode) {
                    StitchingMode.HORIZONTAL -> ImageStitchingEngine.stitchHorizontal(bitmaps, state.commonSettings)
                    StitchingMode.VERTICAL -> ImageStitchingEngine.stitchVertical(bitmaps, state.commonSettings)
                    else -> ImageStitchingEngine.stitchVertical(bitmaps, state.commonSettings)
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            resultBitmap = result,
                            resultDimensions = Pair(result.width, result.height),
                            stage = StitchingPageStage.RESULT_PREVIEW,
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            userMessage = "拼接失败: ${e.localizedMessage ?: "未知错误"}"
                        )
                    }
                }
            }
        }
    }

    private fun saveCurrentResult() {
        val state = _uiState.value
        val context = getApplication<Application>()

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true) }

            val (bitmapToSave, format, quality) = when (state.stage) {
                StitchingPageStage.SUBTITLE_EDITOR -> {
                    val fullSubtitles = ImageStitchingEngine.stitchSubtitles(
                        bitmaps = state.images.map { it.bitmap },
                        settings = state.subtitleSettings,
                        maxPreviewDimension = 0
                    )
                    val q = if (state.subtitleSettings.isCompressed) 82 else 100
                    Triple(fullSubtitles, state.subtitleSettings.outputFormat, q)
                }
                StitchingPageStage.RESULT_PREVIEW -> {
                    val bmp = state.resultBitmap
                    if (bmp == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(isProcessing = false, userMessage = "无可保存的图像") }
                        }
                        return@launch
                    }
                    Triple(bmp, state.commonSettings.outputFormat, 100)
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isProcessing = false) }
                    }
                    return@launch
                }
            }

            val compressFormat = when (format) {
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }

            val filename = "stitch_${System.currentTimeMillis()}.${format.extension}"

            var isSuccess = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Koishi")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val itemUri = resolver.insert(collection, values)

                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { outStream ->
                            bitmapToSave.compress(compressFormat, quality, outStream)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                        isSuccess = true
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Koishi")
                    if (!dir.exists()) dir.mkdirs()
                    val destFile = File(dir, filename)
                    FileOutputStream(destFile).use { outStream ->
                        bitmapToSave.compress(compressFormat, quality, outStream)
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    isSuccess = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = if (isSuccess) "已成功保存至相册 (Pictures/Koishi)" else "保存图像失败"
                    )
                }
            }
        }
    }
}
