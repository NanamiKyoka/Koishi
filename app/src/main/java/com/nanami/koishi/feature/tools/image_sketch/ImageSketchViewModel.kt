package com.nanami.koishi.feature.tools.image_sketch

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
import com.nanami.koishi.feature.tools.image_sketch.engine.ImageSketchEngine
import com.nanami.koishi.feature.tools.image_sketch.engine.ImageSketchParams
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class ImageSketchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageSketchUiState())
    val uiState: StateFlow<ImageSketchUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.sourceBitmap?.recycle()
            _uiState.value.previewSourceBitmap?.recycle()
            _uiState.value.previewBitmap?.recycle()
        } catch (_: Throwable) {}
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: ImageSketchUiEvent) {
        when (event) {
            is ImageSketchUiEvent.OnImageSelected -> loadImage(event.uri)
            is ImageSketchUiEvent.OnClearImage -> clearImage()
            is ImageSketchUiEvent.OnRadiusChanged -> {
                _uiState.update { it.copy(params = it.params.copy(radius = event.radius)) }
                schedulePreviewUpdate()
            }
            is ImageSketchUiEvent.OnResetParams -> {
                _uiState.update { it.copy(params = ImageSketchParams()) }
                schedulePreviewUpdate()
            }
            is ImageSketchUiEvent.OnSaveResult -> saveResultToGallery()
            is ImageSketchUiEvent.OnToggleHelpDialog -> {
                _uiState.update { it.copy(isHelpDialogOpen = event.open) }
            }
            is ImageSketchUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val decoded = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri, MAX_SOURCE_DIMENSION)
            }

            if (decoded == null) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.image_sketch_load_failed)
                    )
                }
                return@launch
            }

            val previewSource = withContext(Dispatchers.IO) {
                downscaleForPreview(decoded)
            }

            _uiState.value.sourceBitmap?.recycle()
            _uiState.value.previewSourceBitmap?.recycle()
            _uiState.value.previewBitmap?.recycle()
            _uiState.update {
                it.copy(
                    sourceUri = uri,
                    sourceBitmap = decoded,
                    previewSourceBitmap = previewSource,
                    previewBitmap = null,
                    isProcessing = false
                )
            }
            schedulePreviewUpdate(immediate = true)
        }
    }

    private fun clearImage() {
        _uiState.value.sourceBitmap?.recycle()
        _uiState.value.previewSourceBitmap?.recycle()
        _uiState.value.previewBitmap?.recycle()
        _uiState.update {
            it.copy(
                sourceUri = null,
                sourceBitmap = null,
                previewSourceBitmap = null,
                previewBitmap = null
            )
        }
    }

    private fun schedulePreviewUpdate(immediate: Boolean = false) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            if (!immediate) {
                delay(PREVIEW_DEBOUNCE_MILLIS)
            }

            val working = _uiState.value.previewSourceBitmap ?: _uiState.value.sourceBitmap
            if (working == null || working.isRecycled) {
                _uiState.update { it.copy(previewBitmap = null) }
                return@launch
            }

            val params = _uiState.value.params
            val rendered = withContext(Dispatchers.Default) {
                try {
                    ImageSketchEngine.createSketch(working, params.radius)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }

            if (rendered != null) {
                val previous = _uiState.value.previewBitmap
                _uiState.update { it.copy(previewBitmap = rendered) }
                if (previous != null && previous != working && previous != rendered) {
                    previous.recycle()
                }
            } else {
                _uiState.update {
                    it.copy(userMessage = getString(R.string.image_sketch_process_failed))
                }
            }
        }
    }

    private fun saveResultToGallery() {
        val state = _uiState.value
        val source = state.sourceBitmap
        if (source == null || source.isRecycled) {
            _uiState.update { it.copy(userMessage = getString(R.string.image_sketch_need_image)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val radius = state.params.radius
            val success = withContext(Dispatchers.IO) {
                try {
                    val sketch = ImageSketchEngine.createSketch(source, radius)
                    val saved = savePng(sketch)
                    sketch.recycle()
                    saved
                } catch (t: Throwable) {
                    t.printStackTrace()
                    false
                }
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = if (success) {
                        getString(R.string.image_sketch_saved_success)
                    } else {
                        getString(R.string.image_sketch_save_failed)
                    }
                )
            }
        }
    }

    private fun downscaleForPreview(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= MAX_PREVIEW_DIMENSION) return source

        val ratio = MAX_PREVIEW_DIMENSION.toFloat() / longest
        val targetWidth = (source.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun decodeSafeBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val context = getApplication<Application>()
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            var sampleSize = 1
            while (rawWidth / sampleSize > maxDimension || rawHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
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

    private fun savePng(bitmap: Bitmap): Boolean {
        val context = getApplication<Application>()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val millis = System.currentTimeMillis() % 1000
        val filename = "ImageSketch_${dateStr}_${String.format(Locale.US, "%03d", millis)}.png"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.IMAGE_SKETCH)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values) ?: return false

                context.contentResolver.openOutputStream(itemUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    AlbumFolders.IMAGE_SKETCH
                )
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, filename)
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    private companion object {
        const val MAX_SOURCE_DIMENSION = 1920
        const val MAX_PREVIEW_DIMENSION = 900
        const val PREVIEW_DEBOUNCE_MILLIS = 60L
    }
}
