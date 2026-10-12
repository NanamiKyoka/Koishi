package com.nanami.koishi.feature.tools.mirage_tank

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.image.io.ImageIoUtil
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
            _uiState.value.sourceImage?.bitmap?.recycle()
            _uiState.value.revealedFront?.recycle()
            _uiState.value.revealedBack?.recycle()
        } catch (_: Throwable) {}
    }

    private fun getString(resId: Int, vararg args: Any): String {
        return getApplication<Application>().getString(resId, *args)
    }

    fun onEvent(event: MirageTankUiEvent) {
        when (event) {
            is MirageTankUiEvent.OnPanelChanged -> {
                _uiState.update { it.copy(panel = event.panel) }
            }

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

            is MirageTankUiEvent.OnSourceImageSelected -> loadSourceImage(event.uri)
            is MirageTankUiEvent.OnClearSourceImage -> clearSourceImage()
            is MirageTankUiEvent.OnSaveRevealedFront -> saveRevealed(front = true)
            is MirageTankUiEvent.OnSaveRevealedBack -> saveRevealed(front = false)

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
                decodeTankImage(uri)
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
                decodeTankImage(uri)
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

    private fun loadSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val item = withContext(Dispatchers.IO) {
                decodeTankImage(uri)
            }
            if (item == null) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.mirage_tank_load_failed)
                    )
                }
                return@launch
            }

            recycleRevealed()
            _uiState.value.sourceImage?.bitmap?.recycle()
            _uiState.update {
                it.copy(
                    sourceImage = item,
                    revealedFront = null,
                    revealedBack = null,
                    isProcessing = false
                )
            }
            restore()
        }
    }

    private fun clearSourceImage() {
        recycleRevealed()
        _uiState.value.sourceImage?.bitmap?.recycle()
        _uiState.update {
            it.copy(
                sourceImage = null,
                revealedFront = null,
                revealedBack = null
            )
        }
    }

    private fun restore() {
        val source = _uiState.value.sourceImage?.bitmap ?: return

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val result = withContext(Dispatchers.Default) {
                try {
                    val width = source.width
                    val height = source.height
                    val pixels = IntArray(width * height)
                    source.getPixels(pixels, 0, width, 0, 0, width, height)

                    val hasTransparency = pixels.any { (it ushr 24) and 0xFF < 255 }
                    val front = MirageTankEngine.simulateDisplayPixels(pixels, width, height, 1f)
                    val back = MirageTankEngine.simulateDisplayPixels(pixels, width, height, 0f)
                    RevealResult(front, back, hasTransparency)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }

            if (result == null) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.mirage_tank_process_failed)
                    )
                }
                return@launch
            }

            val width = source.width
            val height = source.height
            val frontBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(result.frontPixels, 0, width, 0, 0, width, height)
            }
            val backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(result.backPixels, 0, width, 0, 0, width, height)
            }

            recycleRevealed()
            _uiState.update {
                it.copy(
                    revealedFront = frontBitmap,
                    revealedBack = backBitmap,
                    isProcessing = false,
                    userMessage = if (result.hasTransparency) null else getString(R.string.mirage_tank_no_alpha)
                )
            }
        }
    }

    private fun recycleRevealed() {
        try {
            _uiState.value.revealedFront?.recycle()
            _uiState.value.revealedBack?.recycle()
        } catch (_: Throwable) {}
    }

    private fun saveRevealed(front: Boolean) {
        val bitmap = if (front) _uiState.value.revealedFront else _uiState.value.revealedBack
        if (bitmap == null) {
            _uiState.update { it.copy(userMessage = getString(R.string.mirage_tank_no_result_to_save)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = withContext(Dispatchers.IO) {
                ImageIoUtil.savePng(
                    context = getApplication(),
                    bitmap = bitmap,
                    albumFolder = AlbumFolders.MIRAGE_TANK,
                    prefix = if (front) "MirageTankFront" else "MirageTankBack"
                )
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = getString(
                        if (success) {
                            if (front) R.string.mirage_tank_front_saved else R.string.mirage_tank_back_saved
                        } else {
                            R.string.mirage_tank_save_failed
                        }
                    )
                )
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
                ImageIoUtil.savePng(
                    context = getApplication(),
                    bitmap = bitmap,
                    albumFolder = AlbumFolders.MIRAGE_TANK,
                    prefix = "MirageTank"
                )
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

    private fun decodeTankImage(uri: Uri): MirageTankImageItem? {
        val bitmap = ImageIoUtil.decodeBitmap(getApplication(), uri) ?: return null
        return MirageTankImageItem(
            uri = uri,
            bitmap = bitmap,
            width = bitmap.width,
            height = bitmap.height
        )
    }

    private data class RevealResult(
        val frontPixels: IntArray,
        val backPixels: IntArray,
        val hasTransparency: Boolean
    )
}