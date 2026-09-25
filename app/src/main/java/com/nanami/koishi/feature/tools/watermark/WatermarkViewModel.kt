package com.nanami.koishi.feature.tools.watermark

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkConfig
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkEngine
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkFont
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkType
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

class WatermarkViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WatermarkUiState())
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.backgroundBitmap?.recycle()
            _uiState.value.watermarkImageBitmap?.recycle()
            _uiState.value.previewBitmap?.recycle()
        } catch (_: Throwable) {}
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: WatermarkUiEvent) {
        when (event) {
            is WatermarkUiEvent.OnBackgroundSelected -> loadBackgroundImage(event.uri)
            is WatermarkUiEvent.OnClearBackground -> clearBackground()
            is WatermarkUiEvent.OnWatermarkImageSelected -> loadWatermarkImage(event.uri)
            is WatermarkUiEvent.OnClearWatermarkImage -> clearWatermarkImage()
            is WatermarkUiEvent.OnModeChanged -> {
                _uiState.update { it.copy(config = it.config.copy(type = event.mode)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnTextChanged -> {
                _uiState.update { it.copy(config = it.config.copy(text = event.text)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnTextColorChanged -> {
                _uiState.update { it.copy(config = it.config.copy(textColor = event.color)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnTextSizeChanged -> {
                _uiState.update { it.copy(config = it.config.copy(textSize = event.size)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnFontChanged -> {
                _uiState.update { it.copy(config = it.config.copy(font = event.font)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnAlphaChanged -> {
                _uiState.update { it.copy(config = it.config.copy(alpha = event.alpha)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnRotationChanged -> {
                _uiState.update { it.copy(config = it.config.copy(rotation = event.rotation)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnImageScaleChanged -> {
                _uiState.update { it.copy(config = it.config.copy(imageScale = event.scale)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnHorizontalSpacingChanged -> {
                _uiState.update { it.copy(config = it.config.copy(horizontalSpacing = event.spacing)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnVerticalSpacingChanged -> {
                _uiState.update { it.copy(config = it.config.copy(verticalSpacing = event.spacing)) }
                schedulePreviewUpdate()
            }
            is WatermarkUiEvent.OnToggleColorPicker -> {
                _uiState.update { it.copy(showColorPicker = event.show) }
            }
            is WatermarkUiEvent.OnResetConfig -> resetConfig()
            is WatermarkUiEvent.OnSaveResult -> saveResultToGallery()
            is WatermarkUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun loadBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val bitmap = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri, maxDimension = 1920)
            }
            if (bitmap != null) {
                _uiState.value.backgroundBitmap?.recycle()
                _uiState.update {
                    it.copy(
                        backgroundUri = uri,
                        backgroundBitmap = bitmap,
                        isProcessing = false
                    )
                }
                schedulePreviewUpdate(immediate = true)
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.crop_read_failed)
                    )
                }
            }
        }
    }

    private fun clearBackground() {
        _uiState.value.backgroundBitmap?.recycle()
        _uiState.value.previewBitmap?.recycle()
        _uiState.update {
            it.copy(
                backgroundUri = null,
                backgroundBitmap = null,
                previewBitmap = null
            )
        }
    }

    private fun loadWatermarkImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val bitmap = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri, maxDimension = 800)
            }
            if (bitmap != null) {
                _uiState.value.watermarkImageBitmap?.recycle()
                _uiState.update {
                    it.copy(
                        watermarkImageUri = uri,
                        watermarkImageBitmap = bitmap,
                        config = it.config.copy(
                            watermarkBitmap = bitmap,
                            type = WatermarkType.IMAGE
                        ),
                        isProcessing = false
                    )
                }
                schedulePreviewUpdate(immediate = true)
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.crop_read_failed)
                    )
                }
            }
        }
    }

    private fun clearWatermarkImage() {
        _uiState.value.watermarkImageBitmap?.recycle()
        _uiState.update {
            it.copy(
                watermarkImageUri = null,
                watermarkImageBitmap = null,
                config = it.config.copy(watermarkBitmap = null)
            )
        }
        schedulePreviewUpdate(immediate = true)
    }

    private fun resetConfig() {
        val currentWmBmp = _uiState.value.watermarkImageBitmap
        _uiState.update {
            it.copy(
                config = WatermarkConfig(
                    watermarkBitmap = currentWmBmp,
                    type = if (currentWmBmp != null) WatermarkType.IMAGE else WatermarkType.TEXT
                )
            )
        }
        schedulePreviewUpdate(immediate = true)
    }

    private fun schedulePreviewUpdate(immediate: Boolean = false) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            if (!immediate) {
                delay(60) // 防抖节流
            }

            val bg = _uiState.value.backgroundBitmap
            if (bg == null) {
                _uiState.update { it.copy(previewBitmap = null) }
                return@launch
            }

            val config = _uiState.value.config
            val newPreview = withContext(Dispatchers.Default) {
                try {
                    WatermarkEngine.applyWatermark(bg, config)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }

            if (newPreview != null) {
                val oldPreview = _uiState.value.previewBitmap
                _uiState.update { it.copy(previewBitmap = newPreview) }
                if (oldPreview != null && oldPreview != bg) {
                    oldPreview.recycle()
                }
            }
        }
    }

    private fun saveResultToGallery() {
        val state = _uiState.value
        val bgUri = state.backgroundUri

        if (bgUri == null) {
            _uiState.update { it.copy(userMessage = getString(R.string.watermark_need_bg_prompt)) }
            return
        }

        if (state.config.type == WatermarkType.IMAGE && state.config.watermarkBitmap == null) {
            _uiState.update { it.copy(userMessage = getString(R.string.watermark_need_wm_img_prompt)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val context = getApplication<Application>()

            val success = withContext(Dispatchers.IO) {
                try {
                    // 解码原图以确保最高画质保存导出
                    val fullBitmap = decodeSafeBitmap(bgUri, maxDimension = 4096) ?: return@withContext false
                    val watermarkedBitmap = WatermarkEngine.applyWatermark(fullBitmap, state.config)
                    fullBitmap.recycle()

                    val filename = "watermark_${System.currentTimeMillis()}.png"
                    var savedSuccessfully = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.WATERMARK)
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        val itemUri = resolver.insert(collection, values)
                        if (itemUri != null) {
                            resolver.openOutputStream(itemUri)?.use { out ->
                                watermarkedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(itemUri, values, null, null)
                            savedSuccessfully = true
                        }
                    } else {
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), AlbumFolders.WATERMARK)
                        if (!dir.exists()) dir.mkdirs()
                        val destFile = File(dir, filename)
                        FileOutputStream(destFile).use { out ->
                            watermarkedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DATA, destFile.absolutePath)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        }
                        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        savedSuccessfully = true
                    }

                    watermarkedBitmap.recycle()
                    savedSuccessfully
                } catch (t: Throwable) {
                    t.printStackTrace()
                    false
                }
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = if (success) {
                        getString(R.string.watermark_saved_success)
                    } else {
                        getString(R.string.watermark_save_failed)
                    }
                )
            }
        }
    }

    private fun decodeSafeBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val context = getApplication<Application>()
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val rawW = options.outWidth
            val rawH = options.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var inSampleSize = 1
            val maxDim = max(rawW, rawH)
            while (maxDim / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
