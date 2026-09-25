package com.nanami.koishi.feature.tools.mirage_tank

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
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankEngine
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankMode
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class MirageTankViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MirageTankUiState())
    val uiState: StateFlow<MirageTankUiState> = _uiState.asStateFlow()

    private var processJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        try {
            _uiState.value.frontImage?.bitmap?.recycle()
            _uiState.value.backImage?.bitmap?.recycle()
            _uiState.value.resultBitmap?.recycle()
        } catch (_: Throwable) {}
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: MirageTankUiEvent) {
        when (event) {
            is MirageTankUiEvent.OnFrontImageSelected -> loadFrontImage(event.uri)
            is MirageTankUiEvent.OnBackImageSelected -> loadBackImage(event.uri)
            is MirageTankUiEvent.OnClearFrontImage -> clearFrontImage()
            is MirageTankUiEvent.OnClearBackImage -> clearBackImage()
            is MirageTankUiEvent.OnSwapImages -> swapImages()

            is MirageTankUiEvent.OnModeChanged -> {
                _uiState.update { it.copy(params = it.params.copy(mode = event.mode)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnFrontLightnessChanged -> {
                _uiState.update { it.copy(params = it.params.copy(frontLightness = event.value)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnFrontContrastChanged -> {
                _uiState.update { it.copy(params = it.params.copy(frontContrast = event.value)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnBackLightnessChanged -> {
                _uiState.update { it.copy(params = it.params.copy(backLightness = event.value)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnBackContrastChanged -> {
                _uiState.update { it.copy(params = it.params.copy(backContrast = event.value)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnCheckerboardToggled -> {
                _uiState.update { it.copy(params = it.params.copy(enableCheckerboard = event.enabled)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnCheckerboardStrengthChanged -> {
                _uiState.update { it.copy(params = it.params.copy(checkerboardStrength = event.value)) }
                triggerAutoGenerateIfReady()
            }
            is MirageTankUiEvent.OnResetParams -> {
                val currentMode = _uiState.value.params.mode
                _uiState.update { it.copy(params = MirageTankParams(mode = currentMode)) }
                triggerAutoGenerateIfReady()
            }

            is MirageTankUiEvent.OnPreviewBackgroundModeChanged -> {
                _uiState.update { it.copy(previewBackgroundMode = event.mode) }
            }
            is MirageTankUiEvent.OnCustomBackgroundRatioChanged -> {
                _uiState.update { it.copy(customBackgroundRatio = event.ratio) }
            }

            is MirageTankUiEvent.OnGenerateTank -> generateTank()
            is MirageTankUiEvent.OnSaveTank -> saveResultToGallery()
            is MirageTankUiEvent.OnToggleHelpDialog -> {
                _uiState.update { it.copy(isHelpDialogOpen = event.open) }
            }
            is MirageTankUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun loadFrontImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val item = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri)
            }
            if (item != null) {
                _uiState.value.frontImage?.bitmap?.recycle()
                _uiState.update {
                    it.copy(
                        frontImage = item,
                        isProcessing = false
                    )
                }
                triggerAutoGenerateIfReady()
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.mirage_tank_load_failed)
                    )
                }
            }
        }
    }

    private fun loadBackImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val item = withContext(Dispatchers.IO) {
                decodeSafeBitmap(uri)
            }
            if (item != null) {
                _uiState.value.backImage?.bitmap?.recycle()
                _uiState.update {
                    it.copy(
                        backImage = item,
                        isProcessing = false
                    )
                }
                triggerAutoGenerateIfReady()
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.mirage_tank_load_failed)
                    )
                }
            }
        }
    }

    private fun clearFrontImage() {
        _uiState.value.frontImage?.bitmap?.recycle()
        _uiState.value.resultBitmap?.recycle()
        _uiState.update {
            it.copy(
                frontImage = null,
                resultBitmap = null
            )
        }
    }

    private fun clearBackImage() {
        _uiState.value.backImage?.bitmap?.recycle()
        _uiState.value.resultBitmap?.recycle()
        _uiState.update {
            it.copy(
                backImage = null,
                resultBitmap = null
            )
        }
    }

    private fun swapImages() {
        val cur = _uiState.value
        if (cur.frontImage == null && cur.backImage == null) return

        _uiState.update {
            it.copy(
                frontImage = cur.backImage,
                backImage = cur.frontImage
            )
        }
        triggerAutoGenerateIfReady()
    }

    private fun triggerAutoGenerateIfReady() {
        val cur = _uiState.value
        if (cur.hasBothImages) {
            generateTank()
        }
    }

    private fun generateTank() {
        val cur = _uiState.value
        val front = cur.frontImage?.bitmap ?: return
        val back = cur.backImage?.bitmap ?: return

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val result = withContext(Dispatchers.Default) {
                try {
                    MirageTankEngine.createMirageTank(
                        frontBitmap = front,
                        backBitmap = back,
                        params = cur.params
                    )
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }

            if (result != null) {
                _uiState.value.resultBitmap?.recycle()
                _uiState.update {
                    it.copy(
                        resultBitmap = result,
                        isProcessing = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.mirage_tank_process_failed)
                    )
                }
            }
        }
    }

    private fun saveResultToGallery() {
        val cur = _uiState.value
        val bitmap = cur.resultBitmap ?: run {
            _uiState.update { it.copy(userMessage = getString(R.string.mirage_tank_no_result_to_save)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = withContext(Dispatchers.IO) {
                saveLosslessPng(bitmap)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = if (success) {
                        getString(R.string.mirage_tank_saved_success)
                    } else {
                        getString(R.string.mirage_tank_save_failed)
                    }
                )
            }
        }
    }

    private fun decodeSafeBitmap(uri: Uri): MirageTankImageItem? {
        val context = getApplication<Application>()
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            // 限制最大单边尺寸为 1920，既保证极高分辨率细节又避免 OOM
            val maxDim = 1920
            var sampleSize = 1
            var w = origW
            var h = origH
            while (w > maxDim || h > maxDim) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            MirageTankImageItem(
                uri = uri,
                bitmap = decodedBitmap,
                width = decodedBitmap.width,
                height = decodedBitmap.height
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun saveLosslessPng(bitmap: Bitmap): Boolean {
        val context = getApplication<Application>()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val ms = (System.currentTimeMillis() % 1000)
        val filename = "MirageTank_${dateStr}_${String.format(Locale.US, "%03d", ms)}.png"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + AlbumFolders.MIRAGE_TANK)
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
                    AlbumFolders.MIRAGE_TANK
                )
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, filename)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }
}
