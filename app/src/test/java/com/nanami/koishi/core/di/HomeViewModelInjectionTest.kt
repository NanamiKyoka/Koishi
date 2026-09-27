package com.nanami.koishi.core.di

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import com.nanami.koishi.R
import com.nanami.koishi.core.data.repository.SearchHistoryRepository
import com.nanami.koishi.core.data.repository.ToolFavoritesRepository
import com.nanami.koishi.core.data.repository.ToolRepository
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.core.model.ToolItem
import com.nanami.koishi.feature.home.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
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
}
