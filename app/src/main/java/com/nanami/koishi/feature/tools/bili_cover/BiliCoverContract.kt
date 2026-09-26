package com.nanami.koishi.feature.tools.bili_cover

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverResult

sealed interface BiliCoverLoadState {
    data object Idle : BiliCoverLoadState
    data object Loading : BiliCoverLoadState
    data class Success(val result: BiliCoverResult) : BiliCoverLoadState
    data class Error(@StringRes val messageRes: Int, val detail: String? = null) : BiliCoverLoadState
}

data class BiliCoverUiState(
    val input: String = "",
    val loadState: BiliCoverLoadState = BiliCoverLoadState.Idle,
    val recentQueries: List<String> = emptyList(),
    val showHelp: Boolean = false,
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList()
) {
    val canFetch: Boolean
        get() = input.isNotBlank() && loadState !is BiliCoverLoadState.Loading

    val isLoading: Boolean
        get() = loadState is BiliCoverLoadState.Loading

    val currentResult: BiliCoverResult?
        get() = (loadState as? BiliCoverLoadState.Success)?.result
}

sealed interface BiliCoverUiEvent {
    data class OnInputChange(val value: String) : BiliCoverUiEvent
    data object OnFetch : BiliCoverUiEvent
    data class OnSelectRecent(val input: String) : BiliCoverUiEvent
    data object OnClearRecent : BiliCoverUiEvent
    data class OnSaveCover(val index: Int) : BiliCoverUiEvent
    data object OnSaveAllCovers : BiliCoverUiEvent
    data class OnShowHelp(val show: Boolean) : BiliCoverUiEvent
    data object OnDismissMessage : BiliCoverUiEvent
}
