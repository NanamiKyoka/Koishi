package com.nanami.koishi.core.di

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import com.nanami.koishi.R
import com.nanami.koishi.core.data.repository.RecentToolsRepository
import com.nanami.koishi.core.data.repository.SearchHistoryRepository
import com.nanami.koishi.core.data.repository.LaunchTab
import com.nanami.koishi.core.data.repository.NavigationPreferencesRepository
import com.nanami.koishi.core.data.repository.ToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolRepository
import com.nanami.koishi.core.data.repository.ToolVisibilityRepository
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import com.nanami.koishi.feature.home.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class HomeViewModelInjectionTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        androidContext(Application())
        modules(
            viewModelModule,
            module {
                single<ToolRepository> { FakeToolRepository() }
                single<ToolFavoritesRepository> { FakeFavoritesRepository() }
                single<SearchHistoryRepository> { FakeSearchHistoryRepository() }
                single<RecentToolsRepository> { FakeRecentToolsRepository() }
                single<NavigationPreferencesRepository> { FakeNavigationPreferencesRepository() }
                single<ToolVisibilityRepository> { FakeToolVisibilityRepository() }
            }
        )
    }

    @Test
    fun homeViewModelUsesInjectedToolRepository() {
        val viewModel = getKoin().get<HomeViewModel>()

        val tool = viewModel.uiState.value.allTools.single()
        assertEquals("image_obfuscation", tool.id)
        assertEquals(R.string.app_name, tool.nameRes)
    }

    @Test
    fun homeViewModelStartsWithLastMainTab() {
        getKoin().get<NavigationPreferencesRepository>()
            .setLastMainTab(LaunchTab.FAVORITES)

        val viewModel = getKoin().get<HomeViewModel>()

        assertEquals(com.nanami.koishi.feature.home.MainTab.FAVORITES, viewModel.uiState.value.currentTab)
    }

    @Test
    fun homeViewModelFiltersHiddenTools() = runBlocking {
        getKoin().get<ToolVisibilityRepository>()
            .setToolHidden("image_obfuscation", true)

        val viewModel = getKoin().get<HomeViewModel>()

        assertTrue(viewModel.uiState.value.allTools.isEmpty())
    }

    private class FakeToolRepository : ToolRepository {

        private val tools = listOf(
            ToolItem(
                id = "image_obfuscation",
                nameRes = R.string.app_name,
                descriptionRes = R.string.app_name,
                icon = Icons.Rounded.Image,
                category = ToolCategory.IMAGE_APPS,
                isFavorite = false,
                hasDot = false
            )
        )

        override val availableTools: Flow<List<ToolItem>> = MutableStateFlow(tools)

        override fun getToolById(toolId: String): ToolItem? = tools.find { it.id == toolId }
    }

    private class FakeFavoritesRepository : ToolFavoritesRepository {

        private val ids = MutableStateFlow<Set<String>>(emptySet())

        override val favoriteToolIds: Flow<Set<String>> = ids

        override fun isFavorite(toolId: String): Boolean = ids.value.contains(toolId)

        override suspend fun addFavorite(toolId: String) {
            ids.value = ids.value + toolId
        }

        override suspend fun removeFavorite(toolId: String) {
            ids.value = ids.value - toolId
        }

        override suspend fun toggleFavorite(toolId: String) {
            ids.value = if (ids.value.contains(toolId)) ids.value - toolId else ids.value + toolId
        }

        override suspend fun clearAllFavorites() {
            ids.value = emptySet()
        }

        override suspend fun reload() = Unit
    }

    private class FakeRecentToolsRepository : RecentToolsRepository {

        private val ids = MutableStateFlow<List<String>>(emptyList())

        override val recentToolIds: Flow<List<String>> = ids

        override suspend fun recordUsage(toolId: String) {
            if (toolId.isBlank()) return
            ids.value = (listOf(toolId) + ids.value.filter { it != toolId }).take(12)
        }

        override suspend fun clearRecent() {
            ids.value = emptyList()
        }

        override suspend fun reload() = Unit
    }

    private class FakeSearchHistoryRepository : SearchHistoryRepository {

        private val history = MutableStateFlow<List<String>>(emptyList())

        override val searchHistory: Flow<List<String>> = history

        override suspend fun addSearchHistory(keyword: String) {
            val trimmed = keyword.trim()
            if (trimmed.isEmpty()) return
            history.value = listOf(trimmed) + history.value.filter { it != trimmed }
        }

        override suspend fun deleteSearchHistoryItem(keyword: String) {
            history.value = history.value.filter { it != keyword }
        }

        override suspend fun clearAllSearchHistory() {
            history.value = emptyList()
        }

        override suspend fun reload() = Unit
    }

    private class FakeNavigationPreferencesRepository : NavigationPreferencesRepository {

        private var savedTab: LaunchTab = LaunchTab.TOOLBOX

        override val lastMainTab: LaunchTab
            get() = savedTab

        override fun setLastMainTab(tab: LaunchTab) {
            savedTab = tab
        }
    }

    private class FakeToolVisibilityRepository : ToolVisibilityRepository {

        private val ids = MutableStateFlow<Set<String>>(emptySet())

        override val hiddenToolIds: Flow<Set<String>> = ids

        override fun isHidden(toolId: String): Boolean = ids.value.contains(toolId)

        override suspend fun setToolHidden(toolId: String, hidden: Boolean) {
            ids.value = if (hidden) ids.value + toolId else ids.value - toolId
        }

        override suspend fun setHiddenToolIds(toolIds: Set<String>) {
            ids.value = toolIds
        }

        override suspend fun clearHiddenTools() {
            ids.value = emptySet()
        }

        override suspend fun reload() = Unit
    }
}
