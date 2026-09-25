package com.nanami.koishi.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.data.repository.SearchHistoryRepository
import com.nanami.koishi.core.data.repository.SharedPreferencesSearchHistoryRepository
import com.nanami.koishi.core.data.repository.SharedPreferencesToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolRepository
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

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val toolRepository: ToolRepository = InMemoryToolRepository(),
    private val favoritesRepository: ToolFavoritesRepository = SharedPreferencesToolFavoritesRepository(application),
    private val searchHistoryRepository: SearchHistoryRepository = SharedPreferencesSearchHistoryRepository(application)
) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(MainTab.TOOLBOX)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    private val _expandedCategories = MutableStateFlow<Set<ToolCategory>>(
        ToolCategory.entries.filter { it != ToolCategory.ALL }.toSet()
    )

    // 动态将持久化的收藏 ID 注入到可用工具流中
    private val _toolsWithFavoriteState: Flow<List<ToolItem>> = combine(
        toolRepository.availableTools,
        favoritesRepository.favoriteToolIds
    ) { tools, favIds ->
        tools.map { tool ->
            tool.copy(isFavorite = favIds.contains(tool.id))
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(_currentTab, _searchQuery, _isSearchActive) { tab, query, active ->
            Triple(tab, query, active)
        },
        _selectedCategory,
        _expandedCategories,
        _toolsWithFavoriteState,
        searchHistoryRepository.searchHistory
    ) { (tab, query, active), category, expanded, tools, history ->
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
            expandedCategories = expanded,
            searchHistory = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            categories = ToolCategory.entries.filter { it != ToolCategory.ALL },
            allTools = toolRepository.getToolById("image_obfuscation")?.let { listOf(it) } ?: emptyList(),
            filteredTools = toolRepository.getToolById("image_obfuscation")?.let { listOf(it) } ?: emptyList(),
            expandedCategories = ToolCategory.entries.filter { it != ToolCategory.ALL }.toSet()
        )
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnSelectTab -> {
                _currentTab.value = event.tab
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

    fun recordSearchHistory(keyword: String) {
        if (keyword.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryRepository.addSearchHistory(keyword)
            }
        }
    }
}
