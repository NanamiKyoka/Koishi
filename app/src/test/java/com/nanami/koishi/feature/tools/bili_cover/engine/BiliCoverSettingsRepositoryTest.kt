package com.nanami.koishi.feature.tools.bili_cover.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BiliCoverSettingsRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = BiliCoverSettingsRepository(dao)

    @Test
    fun `settings start empty`() = runTest {
        assertTrue(repository.dataFlow.first().recentQueries.isEmpty())
    }

    @Test
    fun `queries are trimmed and kept newest first`() = runTest {
        repository.recordQuery("  BV1GJ411x7h7  ")
        repository.recordQuery("av170001")

        assertEquals(listOf("av170001", "BV1GJ411x7h7"), repository.currentData().recentQueries)
    }

    @Test
    fun `recording the same query moves it to the front once`() = runTest {
        repository.recordQuery("av170001")
        repository.recordQuery("BV1GJ411x7h7")
        repository.recordQuery("av170001")

        assertEquals(listOf("av170001", "BV1GJ411x7h7"), repository.currentData().recentQueries)
    }

    @Test
    fun `blank queries are ignored`() = runTest {
        repository.recordQuery("   ")

        assertTrue(repository.currentData().recentQueries.isEmpty())
    }

    @Test
    fun `history is capped at the declared limit`() = runTest {
        repeat(BiliCoverSettingsRepository.MAX_RECENT + 4) { index ->
            repository.recordQuery("av${index + 1}")
        }

        val queries = repository.currentData().recentQueries
        assertEquals(BiliCoverSettingsRepository.MAX_RECENT, queries.size)
        assertEquals("av${BiliCoverSettingsRepository.MAX_RECENT + 4}", queries.first())
    }

    @Test
    fun `history can be cleared`() = runTest {
        repository.recordQuery("av170001")
        repository.clearRecent()

        assertTrue(repository.currentData().recentQueries.isEmpty())
    }
}
