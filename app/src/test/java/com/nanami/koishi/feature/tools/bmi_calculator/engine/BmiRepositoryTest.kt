package com.nanami.koishi.feature.tools.bmi_calculator.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BmiRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = BmiRepository(dao)

    @Test
    fun `first launch returns an empty default state`() = runTest {
        val data = repository.currentData()

        assertTrue(data.records.isEmpty())
        assertEquals("", data.draftHeightCm)
        assertEquals("", data.draftWeightKg)
    }

    @Test
    fun `adding a record keeps the measurements and seeds the draft`() = runTest {
        val data = repository.addRecord(heightCm = 170.0, weightKg = 65.0, timestamp = 1_000L)

        assertEquals(1, data.records.size)
        assertEquals(170.0, data.records.first().heightCm, 0.0)
        assertEquals(65.0, data.records.first().weightKg, 0.0)
        assertEquals(1_000L, data.records.first().timestamp)
        assertEquals("170", data.draftHeightCm)
        assertEquals("65", data.draftWeightKg)
    }

    @Test
    fun `records accumulate in insertion order`() = runTest {
        repository.addRecord(170.0, 68.0, 1_000L)
        repository.addRecord(170.0, 66.5, 2_000L)
        val data = repository.addRecord(170.0, 65.0, 3_000L)

        assertEquals(listOf(1_000L, 2_000L, 3_000L), data.records.map { it.timestamp })
    }

    @Test
    fun `removing a record only drops the matching entry`() = runTest {
        val first = repository.addRecord(170.0, 68.0, 1_000L).records.first()
        repository.addRecord(170.0, 65.0, 2_000L)

        val data = repository.removeRecord(first.id)

        assertEquals(1, data.records.size)
        assertEquals(2_000L, data.records.first().timestamp)
    }

    @Test
    fun `restoring an entry reinserts it in timestamp order`() = runTest {
        val first = repository.addRecord(170.0, 68.0, 1_000L).records.first()
        repository.addRecord(170.0, 65.0, 2_000L)
        repository.removeRecord(first.id)

        val data = repository.restoreRecord(first)

        assertEquals(listOf(1_000L, 2_000L), data.records.map { it.timestamp })
    }

    @Test
    fun `restoring an existing entry does not duplicate it`() = runTest {
        val record = repository.addRecord(170.0, 68.0, 1_000L).records.first()

        val data = repository.restoreRecord(record)

        assertEquals(1, data.records.size)
    }

    @Test
    fun `clearing keeps the draft but drops every record`() = runTest {
        repository.addRecord(170.0, 68.0, 1_000L)
        repository.addRecord(170.0, 65.0, 2_000L)

        val data = repository.clearRecords()

        assertTrue(data.records.isEmpty())
        assertEquals("170", data.draftHeightCm)
    }

    @Test
    fun `draft updates survive a round trip through storage`() = runTest {
        repository.addRecord(170.0, 68.0, 1_000L)
        repository.updateDraft("168.5", "60.5")

        val reopened = BmiRepository(dao)
        val restored = reopened.currentData()
        val emitted = reopened.dataFlow.first()

        assertEquals(1, restored.records.size)
        assertEquals("168.5", restored.draftHeightCm)
        assertEquals("60.5", restored.draftWeightKg)
        assertEquals(restored, emitted)
    }

    @Test
    fun `corrupted payload falls back to the default state`() = runTest {
        dao.upsert(
            ToolStorageEntity(
                toolId = BmiRepository.TOOL_ID,
                payloadJson = "{ not json",
                updatedAt = 1L
            )
        )

        assertTrue(repository.currentData().records.isEmpty())
    }
}
