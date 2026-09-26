package com.nanami.koishi.feature.home.poetry

data class PoetryUiState(
    val sentence: Hitokoto? = null,
    val isRefreshing: Boolean = false,
    val detailVisible: Boolean = false
)

sealed interface PoetryUiEvent {
    data object OnOpenDetail : PoetryUiEvent
    data object OnDismissDetail : PoetryUiEvent
    data object OnRefresh : PoetryUiEvent
}
