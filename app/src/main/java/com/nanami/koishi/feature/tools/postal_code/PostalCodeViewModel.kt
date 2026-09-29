package com.nanami.koishi.feature.tools.postal_code

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.postal_code.engine.PostalCountries
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDatasetRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDirection
import com.nanami.koishi.feature.tools.postal_code.engine.PostalException
import com.nanami.koishi.feature.tools.postal_code.engine.PostalLoadResult
import com.nanami.koishi.feature.tools.postal_code.engine.PostalRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostalCodeViewModel(
    application: Application,
    private val settings: PostalSettingsRepository,
    private val dataset: PostalDatasetRepository,
    private val repository: PostalRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PostalCodeUiState())
    val uiState: StateFlow<PostalCodeUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var downloadJob: Job? = null

    init {
        refreshDatasetState()
        viewModelScope.launch {
            val stored = settings.currentData()
            _uiState.update {
                it.copy(history = stored.historyOf(it.direction))
            }
        }
    }

    fun onEvent(event: PostalCodeUiEvent) {
        when (event) {
            is PostalCodeUiEvent.OnDirectionChange -> switchDirection(event.direction)

            is PostalCodeUiEvent.OnCountryChange -> {
                _uiState.update {
                    it.copy(countryCode = event.countryCode, loadState = PostalLoadState.Idle)
                }
            }

            is PostalCodeUiEvent.OnQueryInputChange -> {
                _uiState.update { it.copy(queryInput = event.input) }
            }

            is PostalCodeUiEvent.OnSubmit -> search(forceRefresh = false)

            is PostalCodeUiEvent.OnRefresh -> {
                if (_uiState.value.records.isNotEmpty()) search(forceRefresh = true)
            }

            is PostalCodeUiEvent.OnDownloadDataset -> downloadDataset()

            is PostalCodeUiEvent.OnClearDataset -> {
                viewModelScope.launch {
                    dataset.clear()
                    _uiState.update {
                        it.copy(
                            datasetState = PostalDatasetState.Absent,
                            loadState = PostalLoadState.Idle,
                            userMessageRes = R.string.postal_dataset_cleared
                        )
                    }
                }
            }

            is PostalCodeUiEvent.OnHistoryPick -> {
                _uiState.update { it.copy(queryInput = event.keyword) }
                search(forceRefresh = false)
            }

            is PostalCodeUiEvent.OnClearHistory -> {
                viewModelScope.launch {
                    val stored = settings.clearHistory(_uiState.value.direction)
                    _uiState.update {
                        it.copy(
                            history = stored.historyOf(it.direction),
                            userMessageRes = R.string.postal_history_cleared
                        )
                    }
                }
            }

            is PostalCodeUiEvent.OnCopyPostalCode -> {
                _uiState.update { it.copy(userMessageRes = R.string.postal_code_copied) }
            }

            is PostalCodeUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null) }
            }
        }
    }

    private fun switchDirection(direction: PostalDirection) {
        if (direction == _uiState.value.direction) return

        searchJob?.cancel()
        viewModelScope.launch {
            val stored = settings.currentData()
            _uiState.update {
                it.copy(
                    direction = direction,
                    countryCode = PostalCountries.CHINA_CODE,
                    queryInput = "",
                    loadState = PostalLoadState.Idle,
                    history = stored.historyOf(direction)
                )
            }
        }
    }

    private fun refreshDatasetState() {
        viewModelScope.launch {
            val loaded = dataset.load()
            _uiState.update {
                it.copy(
                    datasetState = if (loaded != null) {
                        PostalDatasetState.Ready(loaded.size)
                    } else {
                        PostalDatasetState.Absent
                    }
                )
            }
        }
    }

    private fun downloadDataset() {
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch {
            _uiState.update { it.copy(datasetState = PostalDatasetState.Downloading(0)) }
            try {
                val loaded = dataset.download { percent ->
                    _uiState.update { state ->
                        if (state.datasetState is PostalDatasetState.Downloading) {
                            state.copy(datasetState = PostalDatasetState.Downloading(percent))
                        } else {
                            state
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        datasetState = PostalDatasetState.Ready(loaded.size),
                        userMessageRes = R.string.postal_dataset_ready
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        datasetState = PostalDatasetState.Failed(
                            (error as? PostalException)?.toMessageRes() ?: R.string.postal_error_network
                        )
                    )
                }
            }
        }
    }

    private fun search(forceRefresh: Boolean) {
        val state = _uiState.value
        val keyword = state.queryInput.trim()
        if (keyword.isEmpty() || state.isLoading) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(loadState = PostalLoadState.Loading) }

            val result = repository.query(state.direction, keyword, state.countryCode, forceRefresh)

            when (result) {
                is PostalLoadResult.Success -> {
                    settings.recordQuery(state.direction, keyword)
                    val stored = settings.currentData()
                    _uiState.update {
                        it.copy(
                            loadState = PostalLoadState.Success(
                                records = result.result.records,
                                source = result.result.source,
                                fromCache = result.result.fromCache
                            ),
                            history = stored.historyOf(state.direction)
                        )
                    }
                }

                is PostalLoadResult.Empty -> {
                    _uiState.update { it.copy(loadState = PostalLoadState.Empty) }
                }

                is PostalLoadResult.Failure -> {
                    _uiState.update {
                        it.copy(loadState = PostalLoadState.Error(result.reason.toMessageRes()))
                    }
                }
            }
        }
    }

    private fun PostalException.toMessageRes(): Int = when (this) {
        is PostalException.DatasetUnavailable -> R.string.postal_error_dataset_missing
        is PostalException.NotFound -> R.string.postal_error_not_found
        is PostalException.Network -> R.string.postal_error_network
        is PostalException.Parse -> R.string.postal_error_parse
        is PostalException.ServerError -> R.string.postal_error_server
    }
}
