package com.nanami.koishi.feature.home.poetry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.KoishiApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PoetryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HitokotoRepository(
        HitokotoCache((application as KoishiApp).toolStorageDao)
    )

    private val _uiState = MutableStateFlow(PoetryUiState())
    val uiState: StateFlow<PoetryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onEvent(event: PoetryUiEvent) {
        when (event) {
            is PoetryUiEvent.OnOpenDetail -> _uiState.update { it.copy(detailVisible = true) }
            is PoetryUiEvent.OnDismissDetail -> _uiState.update { it.copy(detailVisible = false) }
            is PoetryUiEvent.OnRefresh -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = it.sentence != null) }

            val sentence = repository.next()
            _uiState.update {
                it.copy(
                    sentence = sentence ?: it.sentence,
                    isRefreshing = false
                )
            }

            viewModelScope.launch { repository.refill() }
        }
    }
}
