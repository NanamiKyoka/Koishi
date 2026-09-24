package com.nanami.koishi.feature.tools.today_in_history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryCache
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryException
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryLoadResult
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryPreferences
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class TodayInHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = HistoryPreferences(application)
    private val repository = HistoryRepository(preferences, HistoryCache(application))

    private val _uiState = MutableStateFlow(TodayInHistoryUiState())
    val uiState: StateFlow<TodayInHistoryUiState> = _uiState.asStateFlow()

    private var loadingJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                isToday = true,
                showApiKeyConfigured = preferences.hasShowApiKey,
                activeSource = preferences.lastUsedSource
            )
        }
        jumpToToday()
    }

    fun onEvent(event: TodayInHistoryUiEvent) {
        when (event) {
            is TodayInHistoryUiEvent.OnRetry -> load(forceRefresh = true)
            is TodayInHistoryUiEvent.OnRefresh -> load(forceRefresh = true)
            is TodayInHistoryUiEvent.OnPreviousDay -> shiftDay(-1)
            is TodayInHistoryUiEvent.OnNextDay -> shiftDay(1)
            is TodayInHistoryUiEvent.OnJumpToToday -> jumpToToday()
            is TodayInHistoryUiEvent.OnToggleEventExpanded -> toggleExpanded(event.key)
            is TodayInHistoryUiEvent.OnToggleAllEvents -> toggleAll(event.expanded)
            is TodayInHistoryUiEvent.OnShowApiKeyDialog -> {
                _uiState.update { it.copy(showApiKeyDialog = event.show) }
            }
            is TodayInHistoryUiEvent.OnSaveShowApiKey -> {
                preferences.saveShowApiAppKey(event.key)
                _uiState.update {
                    it.copy(
                        showApiKeyDialog = false,
                        showApiKeyConfigured = preferences.hasShowApiKey,
                        userMessageRes = R.string.history_key_saved
                    )
                }
                load(forceRefresh = true)
            }
            is TodayInHistoryUiEvent.OnClearShowApiKey -> {
                preferences.saveShowApiAppKey("")
                _uiState.update {
                    it.copy(
                        showApiKeyConfigured = false,
                        userMessageRes = R.string.history_key_cleared
                    )
                }
                load(forceRefresh = true)
            }
            is TodayInHistoryUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null) }
            }
            is TodayInHistoryUiEvent.OnShowDatePicker -> {
                _uiState.update { it.copy(showDatePicker = event.show) }
            }
            is TodayInHistoryUiEvent.OnDatePicked -> {
                _uiState.update {
                    it.copy(
                        month = event.month,
                        day = event.day,
                        isToday = isToday(event.month, event.day),
                        showDatePicker = false,
                        expandedEventKeys = emptySet()
                    )
                }
                load(forceRefresh = false)
            }
        }
    }

    private fun toggleExpanded(key: String) {
        _uiState.update { state ->
            val current = state.expandedEventKeys.toMutableSet()
            if (current.contains(key)) current.remove(key) else current.add(key)
            state.copy(expandedEventKeys = current)
        }
    }

    private fun toggleAll(expanded: Boolean) {
        _uiState.update { state ->
            val keys = if (expanded) {
                state.events.mapIndexed { index, event -> eventKey(index, event) }.toSet()
            } else {
                emptySet()
            }
            state.copy(expandedEventKeys = keys)
        }
    }

    private fun shiftDay(offset: Int) {
        _uiState.update { state ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, state.month - 1)
                set(Calendar.DAY_OF_MONTH, state.day)
                add(Calendar.DAY_OF_MONTH, offset)
            }
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            state.copy(
                month = month,
                day = day,
                isToday = isToday(month, day),
                expandedEventKeys = emptySet()
            )
        }
        load(forceRefresh = false)
    }

    private fun jumpToToday() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        _uiState.update {
            it.copy(
                month = month,
                day = day,
                isToday = true,
                expandedEventKeys = emptySet()
            )
        }
        load(forceRefresh = false)
    }

    private fun isToday(month: Int, day: Int): Boolean {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) + 1 == month &&
                calendar.get(Calendar.DAY_OF_MONTH) == day
    }

    private fun load(forceRefresh: Boolean) {
        loadingJob?.cancel()
        val state = _uiState.value
        val targetMonth = state.month
        val targetDay = state.day

        loadingJob = viewModelScope.launch {
            val keepContent = forceRefresh && state.loadState is HistoryLoadState.Success
            _uiState.update {
                if (keepContent) it.copy(loadState = it.loadState) else it.copy(loadState = HistoryLoadState.Loading)
            }

            val result = repository.load(targetMonth, targetDay, forceRefresh)

            _uiState.update { current ->
                if (current.month != targetMonth || current.day != targetDay) return@update current

                when (result) {
                    is HistoryLoadResult.Success -> current.copy(
                        loadState = HistoryLoadState.Success(result.day, result.fromCache),
                        activeSource = result.day.source,
                        expandedEventKeys = if (result.fromCache) current.expandedEventKeys else emptySet()
                    )
                    is HistoryLoadResult.Empty -> current.copy(loadState = HistoryLoadState.Empty)
                    is HistoryLoadResult.Failure -> current.copy(
                        loadState = HistoryLoadState.Error(
                            messageRes = result.reason.toMessageRes(),
                            detail = result.reason.localizedMessage
                        )
                    )
                }
            }
        }
    }

    private fun HistoryException.toMessageRes(): Int = when (this) {
        is HistoryException.MissingApiKey -> R.string.history_error_missing_key
        is HistoryException.InvalidApiKey -> R.string.history_error_invalid_key
        is HistoryException.QuotaExceeded -> R.string.history_error_quota
        is HistoryException.Network -> R.string.history_error_network
        is HistoryException.Parse -> R.string.history_error_parse
        is HistoryException.ServerError -> R.string.history_error_server
    }

    companion object {
        fun eventKey(index: Int, event: com.nanami.koishi.feature.tools.today_in_history.engine.HistoryEvent): String =
            "$index-${event.year}-${event.title.hashCode()}"
    }
}
