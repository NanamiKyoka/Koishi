package com.nanami.koishi.feature.tools.today_in_history.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySettingsRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = HistorySettingsRepository(dao)

    @Test
    fun `settings start from declared defaults`() = runTest {
        val stored = repository.dataFlow.first()
        assertEquals("", stored.showApiAppKey)
        assertEquals(HistorySource.XXAPI, stored.lastUsedSource)
    }

    @Test
    fun `app key is trimmed and source is recorded independently`() = runTest {
        repository.saveShowApiAppKey("  showapi-key  ")
        repository.recordSource(HistorySource.SHOW_API)

        val stored = repository.currentData()
        assertEquals("showapi-key", stored.showApiAppKey)
        assertEquals(HistorySource.SHOW_API, stored.lastUsedSource)
    }

    @Test
    fun `clearing the app key keeps the last used source`() = runTest {
        repository.saveShowApiAppKey("showapi-key")
        repository.recordSource(HistorySource.SHOW_API)

        repository.saveShowApiAppKey("")

        val stored = repository.currentData()
        assertEquals("", stored.showApiAppKey)
        assertEquals(HistorySource.SHOW_API, stored.lastUsedSource)
    }
}
