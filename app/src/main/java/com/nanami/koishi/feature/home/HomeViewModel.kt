package com.nanami.koishi.feature.home

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(MainTab.TOOLBOX)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    private val _searchHistory = MutableStateFlow(listOf("图片混淆", "番茄混淆"))

    private val _initialTools = listOf(
        ToolItem(
            id = "image_obfuscation",
            nameRes = R.string.tool_image_obfuscation_name,
            descriptionRes = R.string.tool_image_obfuscation_desc,
            icon = Icons.Rounded.Image,
            category = ToolCategory.TEXT_IMAGE,
            isFavorite = true,
            badge = null
        )
    )

    private val _tools = MutableStateFlow(_initialTools)

    val uiState: StateFlow<HomeUiState> = combine(
        _currentTab,
        _searchQuery,
        _isSearchActive,
        _selectedCategory,
        _tools
    ) { tab: MainTab, query: String, active: Boolean, category: ToolCategory, tools: List<ToolItem> ->
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
            allTools = tools,
            filteredTools = filtered,
            favoriteTools = tools.filter { it.isFavorite }
        )
    }.combine(_searchHistory) { state: HomeUiState, history: List<String> ->
        state.copy(searchHistory = history)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(
            allTools = _initialTools,
            filteredTools = _initialTools,
            favoriteTools = _initialTools.filter { it.isFavorite }
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
            }
            is HomeUiEvent.OnClearSearch -> {
                _searchQuery.value = ""
            }
            is HomeUiEvent.OnSelectCategory -> {
                _selectedCategory.value = event.category
            }
            is HomeUiEvent.OnToggleFavorite -> {
                _tools.update { list ->
                    list.map {
                        if (it.id == event.toolId) it.copy(isFavorite = !it.isFavorite) else it
                    }
                }
            }
            is HomeUiEvent.OnSearchHistoryClick -> {
                _searchQuery.value = event.keyword
                _isSearchActive.value = false
            }
            is HomeUiEvent.OnClearHistory -> {
                _searchHistory.value = emptyList()
            }
        }
    }
}
