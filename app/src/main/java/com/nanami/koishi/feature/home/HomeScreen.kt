package com.nanami.koishi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.core.designsystem.adaptive.LocalBottomBarVisibilityController
import com.nanami.koishi.core.designsystem.adaptive.LocalWindowLayoutMode
import com.nanami.koishi.core.designsystem.adaptive.WindowLayoutMode
import com.nanami.koishi.core.model.ToolItem
import com.nanami.koishi.feature.favorites.FavoritesScreen
import com.nanami.koishi.feature.home.components.CategorySectionCard
import com.nanami.koishi.feature.home.components.SearchInputField
import com.nanami.koishi.feature.home.components.SearchResultItem
import com.nanami.koishi.feature.home.poetry.PoetryDetailDialog
import com.nanami.koishi.feature.home.poetry.PoetryHeadline
import com.nanami.koishi.feature.home.poetry.PoetryUiEvent
import com.nanami.koishi.feature.home.poetry.PoetryUiState
import com.nanami.koishi.feature.home.poetry.PoetryViewModel
import com.nanami.koishi.feature.settings.SettingsScreen
import com.nanami.koishi.feature.settings.SettingsViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    poetryViewModel: PoetryViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToTool: (ToolItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val poetryState by poetryViewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        poetryState = poetryState,
        onPoetryEvent = poetryViewModel::onEvent,
        onToolClick = onNavigateToTool,
        settingsContent = {
            SettingsScreen(
                uiState = settingsState,
                onEvent = settingsViewModel::onEvent
            )
        },
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    poetryState: PoetryUiState,
    onPoetryEvent: (PoetryUiEvent) -> Unit,
    onToolClick: (ToolItem) -> Unit,
    settingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecentExpanded by rememberSaveable { mutableStateOf(true) }
    var isFavoritesExpanded by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                MainTab.FAVORITES -> {
                    FavoritesScreen(
                        favoriteTools = uiState.favoriteTools,
                        recentTools = uiState.recentTools,
                        isRecentExpanded = isRecentExpanded,
                        isFavoritesExpanded = isFavoritesExpanded,
                        onToolClick = onToolClick,
                        onToggleFavorite = { onEvent(HomeUiEvent.OnToggleFavorite(it)) },
                        onToggleRecentExpanded = { isRecentExpanded = !isRecentExpanded },
                        onToggleFavoritesExpanded = { isFavoritesExpanded = !isFavoritesExpanded },
                        onNavigateToToolbox = { onEvent(HomeUiEvent.OnSelectTab(MainTab.TOOLBOX)) }
                    )
                }
                MainTab.TOOLBOX -> {
                    ToolboxTabContent(
                        uiState = uiState,
                        onEvent = onEvent,
                        poetryState = poetryState,
                        onPoetryEvent = onPoetryEvent,
                        onToolClick = onToolClick
                    )
                }
                MainTab.SETTINGS -> {
                    settingsContent()
                }
            }
        }
    }

    val sentence = poetryState.sentence
    if (poetryState.detailVisible && sentence != null) {
        PoetryDetailDialog(
            sentence = sentence,
            isRefreshing = poetryState.isRefreshing,
            onRefresh = { onPoetryEvent(PoetryUiEvent.OnRefresh) },
            onDismiss = { onPoetryEvent(PoetryUiEvent.OnDismissDetail) }
        )
    }
}

@Composable
private fun ToolboxTabContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    poetryState: PoetryUiState,
    onPoetryEvent: (PoetryUiEvent) -> Unit,
    onToolClick: (ToolItem) -> Unit
) {
    if (uiState.isSearchActive) {
        ToolSearchContent(
            uiState = uiState,
            onEvent = onEvent,
            onToolClick = onToolClick
        )
    } else {
        ToolboxOverview(
            uiState = uiState,
            onEvent = onEvent,
            poetryState = poetryState,
            onPoetryEvent = onPoetryEvent,
            onToolClick = onToolClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolSearchContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onToolClick: (ToolItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomBarVisibilityController = LocalBottomBarVisibilityController.current
    val scrollModifier = Modifier.nestedScroll(bottomBarVisibilityController.nestedScrollConnection)

    BackHandler(enabled = true) {
        keyboardController?.hide()
        onEvent(HomeUiEvent.OnSearchActiveChange(false))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                SearchInputField(
                    value = uiState.searchQuery,
                    onValueChange = { onEvent(HomeUiEvent.OnSearchQueryChange(it)) },
                    focusRequester = focusRequester,
                    onBack = {
                        keyboardController?.hide()
                        onEvent(HomeUiEvent.OnSearchActiveChange(false))
                    },
                    onClear = {
                        onEvent(HomeUiEvent.OnClearSearch)
                    },
                    onSearch = {
                        onEvent(HomeUiEvent.OnSubmitSearch(uiState.searchQuery))
                        keyboardController?.hide()
                    }
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        if (uiState.searchQuery.isBlank()) {
            AdaptiveContentWidth(maxWidth = 840.dp) {
                SearchSuggestions(
                    history = uiState.searchHistory,
                    onHistoryItemClick = { keyword ->
                        onEvent(HomeUiEvent.OnSearchHistoryClick(keyword))
                    },
                    onDeleteItem = { onEvent(HomeUiEvent.OnDeleteSearchHistoryItem(it)) },
                    onClearHistory = { onEvent(HomeUiEvent.OnClearHistory) },
                    modifier = scrollModifier
                )
            }
        } else {
            val results = uiState.filteredTools
            if (results.isEmpty()) {
                AdaptiveContentWidth(maxWidth = 640.dp) {
                    EmptySearchResult(
                        query = uiState.searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                AdaptiveContentWidth(maxWidth = 960.dp) {
                    SearchResultList(
                        query = uiState.searchQuery,
                        tools = results,
                        onToolClick = { tool ->
                            keyboardController?.hide()
                            onEvent(HomeUiEvent.OnSearchActiveChange(false))
                            onToolClick(tool)
                        },
                        onToggleFavorite = { onEvent(HomeUiEvent.OnToggleFavorite(it)) },
                        modifier = scrollModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultList(
    query: String,
    tools: List<ToolItem>,
    onToolClick: (ToolItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "search_result_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.search_results_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.search_result_for, query),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.tools_count_summary, tools.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        items(
            items = tools,
            key = { it.id }
        ) { tool ->
            SearchResultItem(
                tool = tool,
                onClick = { onToolClick(tool) },
                onToggleFavorite = { onToggleFavorite(tool.id) }
            )
        }
    }
}

@Composable
private fun SearchSuggestions(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (history.isEmpty()) {
            item(key = "empty_history") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ManageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.no_search_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item(key = "history_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_history),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_history),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.clear_history),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item(key = "history_card") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        history.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 44.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onHistoryItemClick(item) }
                                    .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { onDeleteItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.delete_history_item),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResult(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isNotBlank()) {
                stringResource(R.string.empty_search_title, query)
            } else {
                stringResource(R.string.empty_category_title)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_search_tip),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolboxCategoryItem(
    category: com.nanami.koishi.core.model.ToolCategory,
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onToolClick: (ToolItem) -> Unit,
) {
    val toolsInCategory = uiState.allTools.filter { it.category == category }
    CategorySectionCard(
        category = category,
        tools = toolsInCategory,
        isExpanded = uiState.expandedCategories.contains(category),
        onToggleExpand = { onEvent(HomeUiEvent.OnToggleCategoryExpanded(category)) },
        onToolClick = onToolClick,
        onToggleFavorite = { onEvent(HomeUiEvent.OnToggleFavorite(it)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolboxOverview(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    poetryState: PoetryUiState,
    onPoetryEvent: (PoetryUiEvent) -> Unit,
    onToolClick: (ToolItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PoetryHeadline(
                        sentence = poetryState.sentence,
                        onClick = { onPoetryEvent(PoetryUiEvent.OnOpenDetail) },
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            },
            actions = {
                IconButton(onClick = { onEvent(HomeUiEvent.OnSearchActiveChange(true)) }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_icon_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        val categoriesToDisplay = uiState.categories.filter { category ->
            uiState.allTools.any { it.category == category }
        }

        val layoutMode = LocalWindowLayoutMode.current
        val bottomBarVisibilityController = LocalBottomBarVisibilityController.current
        val scrollModifier = Modifier.nestedScroll(bottomBarVisibilityController.nestedScrollConnection)

        AdaptiveContentWidth(
            maxWidth = if (layoutMode == WindowLayoutMode.EXPANDED) 1200.dp else 840.dp,
            modifier = Modifier.weight(1f),
        ) {
            if (categoriesToDisplay.isEmpty()) {
                ToolboxNoVisibleTools(
                    onOpenSettings = { onEvent(HomeUiEvent.OnSelectTab(MainTab.SETTINGS)) }
                )
            } else if (layoutMode == WindowLayoutMode.EXPANDED) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 360.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollModifier),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalItemSpacing = 14.dp,
                ) {
                    items(
                        count = categoriesToDisplay.size,
                        key = { categoriesToDisplay[it].name },
                    ) { index ->
                        ToolboxCategoryItem(
                            category = categoriesToDisplay[index],
                            uiState = uiState,
                            onEvent = onEvent,
                            onToolClick = onToolClick,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollModifier),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        items = categoriesToDisplay,
                        key = { it.name },
                    ) { category ->
                        ToolboxCategoryItem(
                            category = category,
                            uiState = uiState,
                            onEvent = onEvent,
                            onToolClick = onToolClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolboxNoVisibleTools(
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.toolbox_no_visible_tools_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.toolbox_no_visible_tools_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.toolbox_open_settings))
            }
        }
    }
}
