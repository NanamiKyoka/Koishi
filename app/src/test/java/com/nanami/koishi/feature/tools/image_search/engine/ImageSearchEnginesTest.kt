package com.nanami.koishi.feature.tools.image_search.engine

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImageSearchEnginesTest {

    private val sampleImage = ByteArray(64) { it.toByte() }

    @Test
    fun `saucenao is disabled when api key is null`() = runTest {
        val state = ImageSearchEngines.searchSauceNao(sampleImage, null)

        assertEquals(SearchEngineEnum.SAUCENAO, state.engine)
        assertEquals(EngineSearchStatus.CONFIG_REQUIRED, state.status)
        assertEquals(R.string.image_search_saucenao_requires_key, state.errorMessageRes)
        assertTrue("不应返回任何结果", state.results.isEmpty())
    }

    @Test
    fun `saucenao is disabled when api key is blank`() = runTest {
        val state = ImageSearchEngines.searchSauceNao(sampleImage, "   ")

        assertEquals(EngineSearchStatus.CONFIG_REQUIRED, state.status)
        assertEquals(R.string.image_search_saucenao_requires_key, state.errorMessageRes)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `saucenao config required state still exposes fallback url`() = runTest {
        val state = ImageSearchEngines.searchSauceNao(sampleImage, "")

        assertNotNull(state.fallbackUrl)
        assertEquals("https://saucenao.com/", state.fallbackUrl)
    }

    @Test
    fun `google lens state is always actionable`() {
        val state = ImageSearchEngines.createGoogleLensState()

        assertEquals(SearchEngineEnum.GOOGLE_LENS, state.engine)
        assertEquals(EngineSearchStatus.SUCCESS, state.status)
        assertEquals("https://lens.google.com/", state.fallbackUrl)
    }
}
