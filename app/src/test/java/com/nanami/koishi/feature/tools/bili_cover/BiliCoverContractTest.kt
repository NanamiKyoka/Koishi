package com.nanami.koishi.feature.tools.bili_cover

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverAsset
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverResult
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BiliCoverContractTest {

    private val result = BiliCoverResult(
        target = BiliTarget.Video(bvid = "BV1GJ411x7h7"),
        title = "示例视频",
        author = "示例UP主",
        covers = listOf(
            BiliCoverAsset(
                url = "https://i0.hdslb.com/bfs/archive/sample.jpg",
                labelRes = R.string.bili_cover_label_cover
            )
        )
    )

    @Test
    fun `initial state cannot fetch`() {
        val state = BiliCoverUiState()
        assertFalse(state.canFetch)
        assertFalse(state.isLoading)
        assertNull(state.currentResult)
    }

    @Test
    fun `blank input cannot be fetched even with whitespace`() {
        assertFalse(BiliCoverUiState(input = "   ").canFetch)
    }

    @Test
    fun `input is fetchable while idle`() {
        assertTrue(BiliCoverUiState(input = "BV1GJ411x7h7").canFetch)
    }

    @Test
    fun `fetching blocks duplicate requests`() {
        val state = BiliCoverUiState(
            input = "BV1GJ411x7h7",
            loadState = BiliCoverLoadState.Loading
        )
        assertFalse(state.canFetch)
        assertTrue(state.isLoading)
    }

    @Test
    fun `success state exposes the result and stays fetchable`() {
        val state = BiliCoverUiState(
            input = "BV1GJ411x7h7",
            loadState = BiliCoverLoadState.Success(result)
        )
        assertEquals(result, state.currentResult)
        assertTrue(state.canFetch)
        assertFalse(state.isLoading)
    }

    @Test
    fun `error state keeps the input for a retry`() {
        val state = BiliCoverUiState(
            input = "BV1GJ411x7h7",
            loadState = BiliCoverLoadState.Error(R.string.bili_cover_error_network)
        )
        assertTrue(state.canFetch)
        assertNull(state.currentResult)
        assertEquals(BiliCoverLoadState.Error(R.string.bili_cover_error_network), state.loadState)
    }
}
