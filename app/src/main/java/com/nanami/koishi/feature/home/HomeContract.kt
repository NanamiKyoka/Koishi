package com.nanami.koishi.feature.home

import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem

enum class MainTab {
    FAVORITES,
    TOOLBOX,
    SETTINGS
}

data class HomeUiState(
    val currentTab: MainTab = MainTab.TOOLBOX,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedCategory: ToolCategory = ToolCategory.ALL,
    val categories: List<ToolCategory> = emptyList(),
    val allTools: List<ToolItem> = emptyList(),
    val filteredTools: List<ToolItem> = emptyList(),
    val favoriteTools: List<ToolItem> = emptyList(),
    val expandedCategories: Set<ToolCategory> = emptySet(),
    val searchHistory: List<String> = emptyList()
)

sealed interface HomeUiEvent {
    data class OnSelectTab(val tab: MainTab) : HomeUiEvent
    data class OnSearchQueryChange(val query: String) : HomeUiEvent
    data class OnSearchActiveChange(val active: Boolean) : HomeUiEvent
    data object OnClearSearch : HomeUiEvent
    data class OnSelectCategory(val category: ToolCategory) : HomeUiEvent
    data class OnToggleCategoryExpanded(val category: ToolCategory) : HomeUiEvent
    data class OnToggleFavorite(val toolId: String) : HomeUiEvent
    data class OnSearchHistoryClick(val keyword: String) : HomeUiEvent
    data class OnDeleteSearchHistoryItem(val keyword: String) : HomeUiEvent
    data object OnClearHistory : HomeUiEvent
}
