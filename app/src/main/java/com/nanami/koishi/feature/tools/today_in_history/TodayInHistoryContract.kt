package com.nanami.koishi.feature.tools.today_in_history

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryDay
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryEvent
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySource

sealed interface HistoryLoadState {
    data object Idle : HistoryLoadState
    data object Loading : HistoryLoadState
    data class Success(val day: HistoryDay, val fromCache: Boolean) : HistoryLoadState
    data object Empty : HistoryLoadState
    data class Error(@StringRes val messageRes: Int, val detail: String? = null) : HistoryLoadState
}

data class TodayInHistoryUiState(
    val month: Int = 0,
    val day: Int = 0,
    val isToday: Boolean = true,
    val loadState: HistoryLoadState = HistoryLoadState.Idle,
    val expandedEventKeys: Set<String> = emptySet(),
    val showApiKeyDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showApiKeyConfigured: Boolean = false,
    val activeSource: HistorySource = HistorySource.XXAPI,
    @StringRes val userMessageRes: Int? = null
) {
    val events: List<HistoryEvent>
        get() = (loadState as? HistoryLoadState.Success)?.day?.events.orEmpty()

    val isLoading: Boolean
        get() = loadState is HistoryLoadState.Loading
}

sealed interface TodayInHistoryUiEvent {
    data object OnRetry : TodayInHistoryUiEvent
    data object OnRefresh : TodayInHistoryUiEvent
    data object OnPreviousDay : TodayInHistoryUiEvent
    data object OnNextDay : TodayInHistoryUiEvent
    data object OnJumpToToday : TodayInHistoryUiEvent
    data class OnShowDatePicker(val show: Boolean) : TodayInHistoryUiEvent
    data class OnDatePicked(val month: Int, val day: Int) : TodayInHistoryUiEvent
    data class OnToggleEventExpanded(val key: String) : TodayInHistoryUiEvent
    data class OnToggleAllEvents(val expanded: Boolean) : TodayInHistoryUiEvent
    data class OnShowApiKeyDialog(val show: Boolean) : TodayInHistoryUiEvent
    data class OnSaveShowApiKey(val key: String) : TodayInHistoryUiEvent
    data object OnClearShowApiKey : TodayInHistoryUiEvent
    data object OnDismissMessage : TodayInHistoryUiEvent
}
