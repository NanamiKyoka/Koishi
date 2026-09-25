package com.nanami.koishi.feature.tools.image_search

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.KoishiApp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchEngines
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettingsRepository
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = ImageSearchSettingsRepository(
        (application as KoishiApp).toolStorageDao
    )

    private val _uiState = MutableStateFlow(ImageSearchUiState())
    val uiState: StateFlow<ImageSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.dataFlow.collect { stored ->
                _uiState.update { it.copy(sauceNaoApiKey = stored.sauceNaoApiKey) }
            }
        }
    }

    fun onEvent(event: ImageSearchUiEvent) {
        when (event) {
            is ImageSearchUiEvent.OnImageSelected -> handleImageSelected(event.uri)
            is ImageSearchUiEvent.OnClearImage -> {
                _uiState.update {
                    it.copy(
                        selectedImageUri = null,
                        previewBitmap = null,
                        engineStates = emptyMap(),
                        collapsedEngines = emptySet()
                    )
                }
            }
            is ImageSearchUiEvent.OnToggleEngine -> {
                _uiState.update { state ->
                    val current = state.selectedEngines.toMutableSet()
                    if (current.contains(event.engine)) {
                        current.remove(event.engine)
                    } else {
                        current.add(event.engine)
                    }
                    state.copy(selectedEngines = current)
                }
            }
            is ImageSearchUiEvent.OnToggleEngineCollapse -> {
                _uiState.update { state ->
                    val current = state.collapsedEngines.toMutableSet()
                    if (current.contains(event.engine)) {
                        current.remove(event.engine)
                    } else {
                        current.add(event.engine)
                    }
                    state.copy(collapsedEngines = current)
                }
            }
            is ImageSearchUiEvent.OnShowApiKeyDialog -> {
                _uiState.update { it.copy(showApiKeyDialog = event.show) }
            }
            is ImageSearchUiEvent.OnSaveSauceNaoApiKey -> {
                viewModelScope.launch {
                    settings.saveApiKey(event.key)
                    _uiState.update {
                        it.copy(
                            showApiKeyDialog = false,
                            userMessageRes = R.string.image_search_key_saved,
                            userMessageArgs = emptyList()
                        )
                    }
                }
            }
            is ImageSearchUiEvent.OnStartSearch -> startConcurrentSearch()
            is ImageSearchUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null, userMessageArgs = emptyList()) }
            }
        }
    }

    private fun handleImageSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                selectedImageUri = uri,
                                previewBitmap = bitmap,
                                engineStates = emptyMap(),
                                collapsedEngines = emptySet()
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                userMessageRes = R.string.image_search_error_read_image,
                                userMessageArgs = emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            userMessageRes = R.string.image_search_error_load_image,
                            userMessageArgs = listOf(e.localizedMessage ?: "")
                        )
                    }
                }
            }
        }
    }

    private fun startConcurrentSearch() {
        val currentState = _uiState.value
        val bitmap = currentState.previewBitmap
        if (bitmap == null || currentState.selectedImageUri == null) {
            _uiState.update {
                it.copy(
                    userMessageRes = R.string.image_search_need_image,
                    userMessageArgs = emptyList()
                )
            }
            return
        }
        val engines = currentState.selectedEngines
        if (engines.isEmpty()) {
            _uiState.update {
                it.copy(
                    userMessageRes = R.string.image_search_need_engine,
                    userMessageArgs = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                val initialStates = engines.associateWith { engine ->
                    EngineSearchState(
                        engine = engine,
                        status = EngineSearchStatus.SEARCHING
                    )
                }
                state.copy(
                    isSearching = true,
                    engineStates = initialStates,
                    collapsedEngines = emptySet()
                )
            }

            // 获取压缩后的 JPEG 字节
            val imageBytes = withContext(Dispatchers.IO) {
                compressBitmapForUpload(bitmap)
            }

            val apiKey = currentState.sauceNaoApiKey

            // 并发启动每个引擎的搜索协程，先返回先刷新 UI
            val searchJobs = engines.map { engine ->
                async {
                    val resultState = when (engine) {
                        SearchEngineEnum.SAUCENAO -> {
                            ImageSearchEngines.searchSauceNao(imageBytes, apiKey)
                        }
                        SearchEngineEnum.TRACE_MOE -> {
                            ImageSearchEngines.searchTraceMoe(imageBytes)
                        }
                        SearchEngineEnum.ASCII2D -> {
                            ImageSearchEngines.searchAscii2d(imageBytes)
                        }
                        SearchEngineEnum.GOOGLE_LENS -> {
                            ImageSearchEngines.createGoogleLensState()
                        }
                    }
                    _uiState.update { state ->
                        val updatedMap = state.engineStates.toMutableMap()
                        updatedMap[engine] = resultState
                        state.copy(engineStates = updatedMap)
                    }
                }
            }

            searchJobs.awaitAll()
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    private fun compressBitmapForUpload(source: Bitmap): ByteArray {
        val maxDim = 1280
        val width = source.width
        val height = source.height
        val scale = if (width > maxDim || height > maxDim) {
            val maxSide = maxOf(width, height)
            maxDim.toFloat() / maxSide
        } else {
            1.0f
        }

        val targetBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                source,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            source
        }

        val stream = ByteArrayOutputStream()
        targetBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
