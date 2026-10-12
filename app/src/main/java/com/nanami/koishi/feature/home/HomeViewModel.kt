package com.nanami.koishi.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.core.data.repository.RecentToolsRepository
import com.nanami.koishi.core.data.repository.SearchHistoryRepository
import com.nanami.koishi.core.data.repository.LaunchTab
import com.nanami.koishi.core.data.repository.NavigationPreferencesRepository
import com.nanami.koishi.core.data.repository.ToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolRepository
import com.nanami.koishi.core.data.repository.ToolVisibilityRepository
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val toolRepository: ToolRepository,
    private val favoritesRepository: ToolFavoritesRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val recentToolsRepository: RecentToolsRepository,
    private val navigationPreferencesRepository: NavigationPreferencesRepository,
    private val toolVisibilityRepository: ToolVisibilityRepository
) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(navigationPreferencesRepository.lastMainTab.toMainTab())
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    private val _expandedCategories = MutableStateFlow<Set<ToolCategory>>(
        ToolCategory.entries.filter { it != ToolCategory.ALL }.toSet()
    )

    // 动态将持久化的收藏 ID 注入到可用工具流中
    private val _toolsWithFavoriteState: Flow<List<ToolItem>> = combine(
        toolRepository.availableTools,
        favoritesRepository.favoriteToolIds,
        toolVisibilityRepository.hiddenToolIds
    ) { tools, favIds, hiddenIds ->
        tools
            .filterNot { hiddenIds.contains(it.id) }
            .map { tool -> tool.copy(isFavorite = favIds.contains(tool.id)) }
    }

    private val _toolsAndRecent: Flow<Pair<List<ToolItem>, List<ToolItem>>> = combine(
        _toolsWithFavoriteState,
        recentToolsRepository.recentToolIds
    ) { tools, recentIds ->
        val recentTools = recentIds.mapNotNull { id -> tools.find { it.id == id } }
        tools to recentTools
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(_currentTab, _searchQuery, _isSearchActive) { tab, query, active ->
            Triple(tab, query, active)
        },
        _selectedCategory,
        _expandedCategories,
        _toolsAndRecent,
        searchHistoryRepository.searchHistory
    ) { (tab, query, active), category, expanded, (tools, recentTools), history ->
        val app = getApplication<Application>()
        val filtered = tools.filter { tool ->
            val matchesCategory = (category == ToolCategory.ALL) || (tool.category == category)
            val name = app.getString(tool.nameRes)
            val desc = app.getString(tool.descriptionRes)
            val matchesQuery = query.isBlank() ||
                    name.contains(query, ignoreCase = true) ||
                    desc.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        HomeUiState(
            currentTab = tab,
            searchQuery = query,
            isSearchActive = active,
            selectedCategory = category,
            categories = ToolCategory.entries.filter { it != ToolCategory.ALL },
            allTools = tools,
            filteredTools = filtered,
            favoriteTools = tools.filter { it.isFavorite },
            recentTools = recentTools,
            expandedCategories = expanded,
            searchHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            currentTab = _currentTab.value,
            categories = ToolCategory.entries.filter { it != ToolCategory.ALL },
            allTools = toolRepository.getToolById("image_obfuscation")
                ?.takeUnless { toolVisibilityRepository.isHidden(it.id) }
                ?.let { listOf(it) }
                ?: emptyList(),
            filteredTools = toolRepository.getToolById("image_obfuscation")
                ?.takeUnless { toolVisibilityRepository.isHidden(it.id) }
                ?.let { listOf(it) }
                ?: emptyList(),
            expandedCategories = ToolCategory.entries.filter { it != ToolCategory.ALL }.toSet()
        )
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnSelectTab -> {
                _currentTab.value = event.tab
                event.tab.toLaunchTab()?.let(navigationPreferencesRepository::setLastMainTab)
            }
            is HomeUiEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is HomeUiEvent.OnSearchActiveChange -> {
                _isSearchActive.value = event.active
                if (!event.active && _searchQuery.value.isNotBlank()) {
                    recordSearchHistory(_searchQuery.value)
                }
            }
            is HomeUiEvent.OnSubmitSearch -> {
                if (event.query.isNotBlank()) {
                    _searchQuery.value = event.query
                    recordSearchHistory(event.query)
                }
            }
            is HomeUiEvent.OnClearSearch -> {
                _searchQuery.value = ""
            }
            is HomeUiEvent.OnSelectCategory -> {
                _selectedCategory.value = event.category
            }
            is HomeUiEvent.OnToggleCategoryExpanded -> {
                _expandedCategories.update { current ->
                    if (current.contains(event.category)) {
                        current - event.category
                    } else {
                        current + event.category
                    }
                }
            }
            is HomeUiEvent.OnToggleFavorite -> {
                viewModelScope.launch {
                    favoritesRepository.toggleFavorite(event.toolId)
                }
            }
            is HomeUiEvent.OnSearchHistoryClick -> {
                _searchQuery.value = event.keyword
                recordSearchHistory(event.keyword)
            }
            is HomeUiEvent.OnDeleteSearchHistoryItem -> {
                viewModelScope.launch {
                    searchHistoryRepository.deleteSearchHistoryItem(event.keyword)
                }
            }
            is HomeUiEvent.OnClearHistory -> {
                viewModelScope.launch {
                    searchHistoryRepository.clearAllSearchHistory()
                }
            }
        }
    }

    fun recordToolUsage(toolId: String) {
        viewModelScope.launch {
            recentToolsRepository.recordUsage(toolId)
        }
    }

    fun recordSearchHistory(keyword: String) {
        if (keyword.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryRepository.addSearchHistory(keyword)
            }
        }
    }

    private fun LaunchTab.toMainTab(): MainTab = when (this) {
        LaunchTab.FAVORITES -> MainTab.FAVORITES
        LaunchTab.TOOLBOX -> MainTab.TOOLBOX
    }

    private fun MainTab.toLaunchTab(): LaunchTab? = when (this) {
        MainTab.FAVORITES -> LaunchTab.FAVORITES
        MainTab.TOOLBOX -> LaunchTab.TOOLBOX
        MainTab.SETTINGS -> null
    }
}
