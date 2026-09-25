package com.nanami.koishi.feature.tools.today_in_history.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCacheTest {

    private val dao = FakeToolStorageDao()
    private val cache = HistoryCache(dao)

    private fun stamp(month: Int, day: Int, year: Int = 2026) =
        HistoryCacheStamp(dateKey = "%02d-%02d".format(month, day), year = year)

    private fun day(month: Int, day: Int, source: HistorySource = HistorySource.SHOW_API) =
        HistoryDay(
            month = month,
            day = day,
            events = listOf(HistoryEvent("某事件", 1999, month, day, "内容", "https://example.com/a.png")),
            source = source
        )

    @Test
    fun `written day is readable within the same year`() = runTest {
        val source = day(9, 25)
        val stamp = stamp(9, 25)

        cache.writeDay(source, stamp)

        assertEquals(source, cache.readDay(9, 25, stamp))
        assertFalse(cache.isEmptyResultCached(9, 25, stamp))
    }

    @Test
    fun `empty result is cached and told apart from a missing entry`() = runTest {
        val stamp = stamp(9, 25)

        assertFalse(cache.isEmptyResultCached(9, 25, stamp))

        cache.writeEmpty(9, 25, stamp)

        assertTrue(cache.isEmptyResultCached(9, 25, stamp))
        assertNull(cache.readDay(9, 25, stamp))
    }

    @Test
    fun `cache from an earlier year is treated as a miss`() = runTest {
        cache.writeDay(day(9, 25), stamp(9, 25, year = 2025))

        assertNull(cache.readDay(9, 25, stamp(9, 25, year = 2026)))
        assertFalse(cache.isEmptyResultCached(9, 25, stamp(9, 25, year = 2026)))
    }

    @Test
    fun `other dates are unaffected`() = runTest {
        cache.writeDay(day(9, 25), stamp(9, 25))

        assertNull(cache.readDay(10, 1, stamp(10, 1)))
        assertFalse(cache.isEmptyResultCached(10, 1, stamp(10, 1)))
    }

    @Test
    fun `corrupted payload is treated as a cache miss`() = runTest {
        dao.upsert(
            ToolStorageEntity(
                toolId = "today_in_history:day:09-25",
                payloadJson = "{broken",
                updatedAt = 0L
            )
        )

        assertNull(cache.readDay(9, 25, stamp(9, 25)))
        assertFalse(cache.isEmptyResultCached(9, 25, stamp(9, 25)))
    }

    @Test
    fun `fallback source survives the round trip`() = runTest {
        val source = day(9, 25, source = HistorySource.XXAPI)

        cache.writeDay(source, stamp(9, 25))

        assertEquals(HistorySource.XXAPI, cache.readDay(9, 25, stamp(9, 25))?.source)
    }
}
