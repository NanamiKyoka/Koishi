package com.nanami.koishi.feature.tools.image_search

import android.net.Uri
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImageSearchContractTest {

    @Test
    fun testInitialUiState() {
        val state = ImageSearchUiState()
        assertEquals(3, state.selectedEngines.size)
        assertFalse("无图片时不能搜索", state.canSearch)
        assertFalse("初始无结果", state.hasResults)
        assertFalse(state.isSearching)
    }

    @Test
    fun testCanSearchConditions() {
        val mockUri = android.net.FakeUri()

        // 有图片且有引擎
        val readyState = ImageSearchUiState(
            selectedImageUri = mockUri,
            selectedEngines = setOf(SearchEngineEnum.SAUCENAO)
        )
        assertTrue(readyState.canSearch)

        // 正在搜索中不可重复点搜索
        val searchingState = readyState.copy(isSearching = true)
        assertFalse(searchingState.canSearch)

        // 无选中引擎时不可搜索
        val noEngineState = readyState.copy(selectedEngines = emptySet())
        assertFalse(noEngineState.canSearch)
    }

    @Test
    fun testHasResultsDetection() {
        val item = SearchResultItem(
            engine = SearchEngineEnum.SAUCENAO,
            title = "Sample",
            similarity = 90.0f
        )
        val stateWithResults = ImageSearchUiState(
            engineStates = mapOf(
                SearchEngineEnum.SAUCENAO to EngineSearchState(
                    engine = SearchEngineEnum.SAUCENAO,
                    status = EngineSearchStatus.SUCCESS,
                    results = listOf(item)
                )
            )
        )
        assertTrue(stateWithResults.hasResults)
    }

    @Test
    fun testSearchEnginesEnumCompleteness() {
        val engines = SearchEngineEnum.entries
        assertEquals(3, engines.size)
        assertTrue(engines.any { it == SearchEngineEnum.SAUCENAO })
        assertTrue(engines.any { it == SearchEngineEnum.TRACE_MOE })
        assertTrue(engines.any { it == SearchEngineEnum.GOOGLE_LENS })
    }
}
