package com.nanami.koishi.core.data.storage

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
private data class Counter(val value: Int = 0)

private class CounterRepository(dao: ToolStorageDao) : BaseToolRepository<Counter>(
    toolId = TOOL_ID,
    serializer = Counter.serializer(),
    dao = dao,
    defaultData = Counter()
) {
    var lastDecodeError: Exception? = null

    override fun onDecodeFailed(error: Exception) {
        lastDecodeError = error
    }

    companion object {
        const val TOOL_ID = "counter"
    }
}

class BaseToolRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = CounterRepository(dao)

    @Test
    fun `missing record falls back to default data`() = runTest {
        assertEquals(Counter(), repository.currentData())
        assertEquals(Counter(), repository.dataFlow.first())
        assertFalse(repository.hasStoredData())
    }

    @Test
    fun `updateData persists payload and emits through dataFlow`() = runTest {
        repository.updateData { it.copy(value = 7) }

        assertEquals(7, repository.currentData().value)
        assertEquals(7, repository.dataFlow.first().value)
        assertTrue(repository.hasStoredData())

        val entity = dao.find(CounterRepository.TOOL_ID)
        assertNotNull(entity)
        assertEquals("""{"value":7}""", entity?.payloadJson)
        assertTrue((entity?.updatedAt ?: 0L) > 0L)
    }

    @Test
    fun `updateData skips write when value is unchanged`() = runTest {
        repository.updateData { it.copy(value = 3) }
        val writesAfterFirstUpdate = dao.upsertCount

        repository.updateData { it.copy(value = 3) }

        assertEquals(writesAfterFirstUpdate, dao.upsertCount)
    }

    @Test
    fun `concurrent updateData keeps every write`() = runTest {
        dao.readDelayMillis = 1

        coroutineScope {
            List(64) { async { repository.updateData { it.copy(value = it.value + 1) } } }.awaitAll()
        }

        assertEquals(64, repository.currentData().value)
    }

    @Test
    fun `corrupted payload falls back to default and reports error`() = runTest {
        dao.upsert(ToolStorageEntity(CounterRepository.TOOL_ID, "{not-json", 1L))

        assertEquals(Counter(), repository.currentData())
        assertEquals(Counter(), repository.dataFlow.first())
        assertNotNull(repository.lastDecodeError)
    }

    @Test
    fun `unknown fields in payload are tolerated`() = runTest {
        dao.upsert(ToolStorageEntity(CounterRepository.TOOL_ID, """{"value":5,"legacy":true}""", 1L))

        assertEquals(5, repository.currentData().value)
    }

    @Test
    fun `reset clears stored record`() = runTest {
        repository.setData(Counter(value = 9))

        assertTrue(repository.reset())
        assertFalse(repository.hasStoredData())
        assertEquals(Counter(), repository.currentData())
    }
}
