package com.nanami.koishi.feature.tools.decision_maker.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val storage = DecisionStorageRepository(dao)
    private val presets = listOf(
        DecisionTopic(
            id = "builtin_meal",
            title = "今天吃什么",
            options = listOf(
                DecisionOption(id = "builtin_meal:0", text = "火锅"),
                DecisionOption(id = "builtin_meal:1", text = "烧烤")
            )
        )
    )
    private val repository = DecisionRepository(builtInTopics = { presets }, storage = storage)

    private suspend fun topicIds(): List<String> = repository.topics.first().map { it.id }

    @Test
    fun `built-in preset is available before any user change`() = runTest {
        assertEquals(listOf("builtin_meal"), topicIds())
        assertEquals(DecisionMode.WHEEL, storage.currentData().mode)
        assertTrue(storage.currentData().hapticsEnabled)
    }

    @Test
    fun `editing built-in stores an override instead of a second topic`() = runTest {
        repository.saveTopic(presets.single().copy(title = "  今晚吃什么  "))

        val stored = storage.currentData()
        assertEquals(1, stored.storedTopics.size)
        assertEquals("builtin_meal", stored.storedTopics.single().id)
        assertEquals(listOf("builtin_meal"), topicIds())
        assertEquals("今晚吃什么", repository.topics.first().single().title)
    }

    @Test
    fun `deleting built-in hides it and restore brings it back`() = runTest {
        repository.deleteTopic("builtin_meal")

        assertTrue(topicIds().isEmpty())
        assertTrue(storage.currentData().hiddenBuiltInIds.contains("builtin_meal"))

        repository.restoreBuiltIns()

        assertEquals(listOf("builtin_meal"), topicIds())
        assertTrue(storage.currentData().hiddenBuiltInIds.isEmpty())
    }

    @Test
    fun `custom topic follows built-ins and is removed on delete`() = runTest {
        repository.saveTopic(
            DecisionTopic(
                id = "topic_1",
                title = "周末计划",
                options = listOf(DecisionOption(id = "opt_1", text = "爬山", weight = 3))
            )
        )

        assertEquals(listOf("builtin_meal", "topic_1"), topicIds())
        assertEquals(3, repository.topics.first().last().options.single().weight)

        repository.deleteTopic("topic_1")

        assertEquals(listOf("builtin_meal"), topicIds())
        assertTrue(storage.currentData().storedTopics.isEmpty())
        assertTrue(storage.currentData().hiddenBuiltInIds.isEmpty())
    }

    @Test
    fun `saveTopic trims title and drops blank or duplicate options`() = runTest {
        val saved = repository.saveTopic(
            DecisionTopic(
                id = "topic_1",
                title = "  计划  ",
                options = listOf(
                    DecisionOption(id = "a", text = " 爬山 "),
                    DecisionOption(id = "b", text = "   "),
                    DecisionOption(id = "c", text = "爬山")
                )
            )
        )

        assertEquals("计划", saved.title)
        assertEquals(listOf("爬山"), saved.options.map { it.text })
    }

    @Test
    fun `selection mode and haptics are persisted`() = runTest {
        repository.selectTopic("topic_1")
        repository.setMode(DecisionMode.FORTUNE_STICK)
        repository.setHapticsEnabled(false)

        val stored = storage.currentData()
        assertEquals("topic_1", stored.selectedTopicId)
        assertEquals(DecisionMode.FORTUNE_STICK, stored.mode)
        assertFalse(stored.hapticsEnabled)
    }

    @Test
    fun `export contains built-in topics`() = runTest {
        val decoded = DecisionArchiveCodec.decode(repository.exportJson())

        assertEquals(listOf("builtin_meal"), decoded?.map { it.id })
    }

    @Test
    fun `import adds new topics and replaces existing ones`() = runTest {
        val added = repository.importJson(
            DecisionArchiveCodec.encode(
                listOf(DecisionTopic("topic_1", "甲", listOf(DecisionOption("o1", "A"))))
            )
        )
        assertEquals(1, (added as TopicImportResult.Success).added)
        assertEquals(0, added.replaced)

        val replaced = repository.importJson(
            DecisionArchiveCodec.encode(
                listOf(DecisionTopic("topic_1", "乙", listOf(DecisionOption("o1", "A"))))
            )
        )
        assertEquals(0, (replaced as TopicImportResult.Success).added)
        assertEquals(1, replaced.replaced)
        assertEquals("乙", repository.topics.first().last().title)
    }

    @Test
    fun `import restores a previously deleted built-in`() = runTest {
        repository.deleteTopic("builtin_meal")

        repository.importJson(
            DecisionArchiveCodec.encode(listOf(presets.single().copy(title = "重新导入")))
        )

        assertTrue(storage.currentData().hiddenBuiltInIds.isEmpty())
        assertEquals("重新导入", repository.topics.first().single().title)
    }

    @Test
    fun `malformed and empty archives are reported`() = runTest {
        assertTrue(repository.importJson("not a json") is TopicImportResult.Malformed)
        assertTrue(repository.importJson(DecisionArchiveCodec.encode(emptyList())) is TopicImportResult.Empty)
    }
}
