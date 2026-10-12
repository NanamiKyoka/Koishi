package com.nanami.koishi.feature.tools.prism_tank

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.image.io.ImageIoUtil
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankEngine
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankParams
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankRevealConfig
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrismTankViewModel(
    application: Application,
    private val settingsRepository: PrismTankSettingsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PrismTankUiState())
    val uiState: StateFlow<PrismTankUiState> = _uiState.asStateFlow()

    private var processJob: Job? = null
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = settingsRepository.currentData()
            _uiState.update { it.copy(params = stored.params, revealConfig = stored.revealConfig) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val state = _uiState.value
        listOfNotNull(
            state.innerImage?.bitmap,
            state.coverImage?.bitmap,
            state.resultBitmap,
            state.sourceImage?.bitmap,
            state.revealedInner,
            state.revealedCover
        ).forEach { it.recycle() }
    }

    fun onEvent(event: PrismTankUiEvent) {
        when (event) {
            is PrismTankUiEvent.OnPanelChanged -> _uiState.update { it.copy(panel = event.panel) }

            is PrismTankUiEvent.OnInnerImageSelected -> loadImage(event.uri, isCover = false)
            is PrismTankUiEvent.OnCoverImageSelected -> loadImage(event.uri, isCover = true)
            is PrismTankUiEvent.OnClearInnerImage -> clearImage(isCover = false)
            is PrismTankUiEvent.OnClearCoverImage -> clearImage(isCover = true)
            is PrismTankUiEvent.OnSwapImages -> swapImages()

            is PrismTankUiEvent.OnInnerThresholdChanged -> updateParams { it.copy(innerThreshold = event.value) }
            is PrismTankUiEvent.OnCoverThresholdChanged -> updateParams { it.copy(coverThreshold = event.value) }
            is PrismTankUiEvent.OnGapChanged -> updateParams { it.copy(gap = event.value) }
            is PrismTankUiEvent.OnSlopeChanged -> updateParams { it.copy(slope = event.value) }
            is PrismTankUiEvent.OnInnerGrayToggled -> updateParams { it.copy(isInnerGray = event.enabled) }
            is PrismTankUiEvent.OnCoverGrayToggled -> updateParams { it.copy(isCoverGray = event.enabled) }
            is PrismTankUiEvent.OnReverseToggled -> updateParams { it.copy(isReverse = event.enabled) }
            is PrismTankUiEvent.OnResetParams -> updateParams { PrismTankParams() }

            is PrismTankUiEvent.OnExposureGainChanged -> {
                _uiState.update { it.copy(exposureGain = event.value) }
            }

            is PrismTankUiEvent.OnSourceImageSelected -> loadSourceImage(event.uri)
            is PrismTankUiEvent.OnClearSourceImage -> clearSourceImage()
            is PrismTankUiEvent.OnRevealLowerChanged -> updateRevealConfig { it.copy(lowerThreshold = event.value) }
            is PrismTankUiEvent.OnRevealUpperChanged -> updateRevealConfig { it.copy(higherThreshold = event.value) }
            is PrismTankUiEvent.OnRevealContrastChanged -> updateRevealConfig { it.copy(contrast = event.value) }
            is PrismTankUiEvent.OnRevealFillChanged -> updateRevealConfig { it.copy(fill = event.fill) }
            is PrismTankUiEvent.OnResetRevealConfig -> updateRevealConfig { PrismTankRevealConfig() }
            is PrismTankUiEvent.OnReveal -> reveal()
            is PrismTankUiEvent.OnSaveRevealedInner -> saveRevealed(revealedInner = true)
            is PrismTankUiEvent.OnSaveRevealedCover -> saveRevealed(revealedInner = false)

            is PrismTankUiEvent.OnGenerateTank -> generateTank()
            is PrismTankUiEvent.OnSaveTank -> saveTank()
            is PrismTankUiEvent.OnToggleHelpDialog -> _uiState.update { it.copy(isHelpDialogOpen = event.open) }
            is PrismTankUiEvent.OnDismissMessage -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private fun updateParams(transform: (PrismTankParams) -> PrismTankParams) {
        _uiState.update { it.copy(params = transform(it.params)) }
        triggerAutoGenerateIfReady()
        schedulePersist()
    }

    private fun updateRevealConfig(transform: (PrismTankRevealConfig) -> PrismTankRevealConfig) {
        _uiState.update { it.copy(revealConfig = transform(it.revealConfig)) }
        triggerAutoRevealIfReady()
        schedulePersist()
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DELAY_MILLIS)
            val state = _uiState.value
            settingsRepository.save(state.params, state.revealConfig)
        }
    }

    private fun loadImage(uri: Uri, isCover: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val item = withContext(Dispatchers.IO) {
                ImageIoUtil.decodeBitmap(getApplication(), uri)?.let {
                    PrismTankImageItem(uri = uri, bitmap = it, width = it.width, height = it.height)
                }
            }
            if (item == null) {
                _uiState.update {
                    it.copy(isProcessing = false, userMessage = getString(R.string.prism_tank_load_failed))
                }
                return@launch
            }

            val replaced = if (isCover) _uiState.value.coverImage else _uiState.value.innerImage
            replaced?.bitmap?.recycle()
            _uiState.update {
                if (isCover) it.copy(coverImage = item, isProcessing = false)
                else it.copy(innerImage = item, isProcessing = false)
            }
            triggerAutoGenerateIfReady()
        }
    }

    private fun clearImage(isCover: Boolean) {
        val state = _uiState.value
        if (isCover) state.coverImage?.bitmap?.recycle() else state.innerImage?.bitmap?.recycle()
        state.resultBitmap?.recycle()

        _uiState.update {
            if (isCover) it.copy(coverImage = null, resultBitmap = null)
            else it.copy(innerImage = null, resultBitmap = null)
        }
    }

    private fun swapImages() {
        val state = _uiState.value
        if (state.innerImage == null && state.coverImage == null) return

        _uiState.update { it.copy(innerImage = state.coverImage, coverImage = state.innerImage) }
        triggerAutoGenerateIfReady()
    }

    private fun triggerAutoGenerateIfReady() {
        if (_uiState.value.hasBothImages) generateTank()
    }

    private fun generateTank() {
        val state = _uiState.value
        val inner = state.innerImage?.bitmap ?: return
        val cover = state.coverImage?.bitmap ?: return

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.Default) {
                try {
                    PrismTankEngine.createPrismTank(
                        innerBitmap = inner,
                        coverBitmap = cover,
                        params = state.params
                    )
                } catch (error: Throwable) {
                    error.printStackTrace()
                    null
                }
            }

            if (result == null) {
                _uiState.update {
                    it.copy(isProcessing = false, userMessage = getString(R.string.prism_tank_process_failed))
                }
                return@launch
            }

            _uiState.value.resultBitmap?.recycle()
            _uiState.update { it.copy(resultBitmap = result, isProcessing = false) }
        }
    }

    private fun loadSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val item = withContext(Dispatchers.IO) {
                ImageIoUtil.decodeBitmap(getApplication(), uri)?.let {
                    PrismTankImageItem(uri = uri, bitmap = it, width = it.width, height = it.height)
                }
            }
            if (item == null) {
                _uiState.update {
                    it.copy(isProcessing = false, userMessage = getString(R.string.prism_tank_load_failed))
                }
                return@launch
            }

            _uiState.value.sourceImage?.bitmap?.recycle()
            _uiState.update { it.copy(sourceImage = item, isProcessing = false) }
            reveal()
        }
    }

    private fun clearSourceImage() {
        val state = _uiState.value
        state.sourceImage?.bitmap?.recycle()
        state.revealedInner?.recycle()
        state.revealedCover?.recycle()
        _uiState.update { it.copy(sourceImage = null, revealedInner = null, revealedCover = null) }
    }

    private fun triggerAutoRevealIfReady() {
        if (_uiState.value.sourceImage != null) reveal()
    }

    private fun reveal() {
        val source = _uiState.value.sourceImage?.bitmap ?: return
        val config = _uiState.value.revealConfig

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.Default) {
                try {
                    val width = source.width
                    val height = source.height
                    val pixels = IntArray(width * height)
                    source.getPixels(pixels, 0, width, 0, 0, width, height)

                    val innerPixels = PrismTankEngine.revealInnerPixels(pixels, width, height, config)
                    val coverPixels = PrismTankEngine.revealCoverPixels(pixels, width, height, config)

                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        setPixels(innerPixels, 0, width, 0, 0, width, height)
                    } to Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                        setPixels(coverPixels, 0, width, 0, 0, width, height)
                    }
                } catch (error: Throwable) {
                    error.printStackTrace()
                    null
                }
            }

            if (result == null) {
                _uiState.update {
                    it.copy(isProcessing = false, userMessage = getString(R.string.prism_tank_process_failed))
                }
                return@launch
            }

            _uiState.value.revealedInner?.recycle()
            _uiState.value.revealedCover?.recycle()
            _uiState.update {
                it.copy(revealedInner = result.first, revealedCover = result.second, isProcessing = false)
            }
        }
    }

    private fun saveTank() {
        val bitmap = _uiState.value.resultBitmap ?: run {
            _uiState.update { it.copy(userMessage = getString(R.string.prism_tank_no_result_to_save)) }
            return
        }
        save(bitmap, R.string.prism_tank_saved_success, "PrismTank")
    }

    private fun saveRevealed(revealedInner: Boolean) {
        val state = _uiState.value
        val bitmap = if (revealedInner) state.revealedInner else state.revealedCover
        if (bitmap == null) {
            _uiState.update { it.copy(userMessage = getString(R.string.prism_tank_no_result_to_save)) }
            return
        }
        val successRes = if (revealedInner) R.string.prism_tank_inner_saved else R.string.prism_tank_cover_saved
        val prefix = if (revealedInner) "PrismTankInner" else "PrismTankCover"
        save(bitmap, successRes, prefix)
    }

    private fun save(bitmap: Bitmap, successRes: Int, prefix: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = withContext(Dispatchers.IO) {
                ImageIoUtil.savePng(getApplication(), bitmap, AlbumFolders.PRISM_TANK, prefix)
            }
            _uiState.update {
                it.copy(
                    isSaving = false,
                    userMessage = getString(if (success) successRes else R.string.prism_tank_save_failed)
                )
            }
        }
    }

    private companion object {
        const val PERSIST_DELAY_MILLIS = 400L
    }
}