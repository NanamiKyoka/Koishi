package com.nanami.koishi.feature.tools.decision_maker.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionTopicTest {

    @Test
    fun sanitized_trimsAndDropsBlankAndDuplicateOptions() {
        val topic = DecisionTopic(
            id = "custom",
            title = "  今天吃什么  ",
            options = listOf(
                DecisionOption("1", " 火锅 ", 3),
                DecisionOption("2", "火锅", 7),
                DecisionOption("3", "   ", 5),
                DecisionOption("4", "麻辣烫", 0)
            )
        )

        val sanitized = topic.sanitized()

        assertEquals("今天吃什么", sanitized.title)
        assertEquals(2, sanitized.options.size)
        assertEquals("火锅", sanitized.options[0].text)
        assertEquals(3, sanitized.options[0].weight)
        assertEquals(DecisionWeight.MIN, sanitized.options[1].weight)
    }

    @Test
    fun sanitized_clampsWeightToConfiguredRange() {
        val topic = DecisionTopic(
            id = "custom",
            title = "测试",
            options = listOf(
                DecisionOption("1", "低", -12),
                DecisionOption("2", "高", 42)
            )
        )

        val sanitized = topic.sanitized()

        assertEquals(DecisionWeight.MIN, sanitized.options[0].weight)
        assertEquals(DecisionWeight.MAX, sanitized.options[1].weight)
    }

    @Test
    fun builtInTopic_isRecognisedByPresetId() {
        val builtIn = DecisionTopic(BuiltInPresets.MEAL_ID, "今天吃什么")
        val custom = DecisionTopic("topic_abc", "我的主题")

        assertTrue(builtIn.isBuiltIn)
        assertEquals(false, custom.isBuiltIn)
    }

    @Test
    fun playableOptions_ignoreBlankEntries() {
        val topic = DecisionTopic(
            id = "custom",
            title = "测试",
            options = listOf(
                DecisionOption("a", "火锅"),
                DecisionOption("b", "烤肉"),
                DecisionOption("c", "  ")
            )
        )

        assertEquals(2, topic.playableOptions.size)
        assertEquals(listOf("火锅", "烤肉"), topic.playableOptions.map { it.text })
    }
}

class DecisionArchiveCodecTest {

    private val topics = listOf(
        DecisionTopic(
            id = "custom_one",
            title = "我的主题",
            options = listOf(
                DecisionOption("o1", "火锅", 3),
                DecisionOption("o2", "烤肉", 10)
            )
        ),
        DecisionTopic(id = BuiltInPresets.MEAL_ID, title = "今天吃什么", options = emptyList())
    )

    @Test
    fun encodeThenDecode_keepsTopicsAndWeights() {
        val decoded = DecisionArchiveCodec.decode(DecisionArchiveCodec.encode(topics))

        assertNotNull(decoded)
        assertEquals(topics, decoded)
    }

    @Test
    fun decode_toleratesUnknownFieldsAndMissingWeights() {
        val raw = """
            {
              "version": 1,
              "exportedAt": "2026-09-25",
              "topics": [
                {"id": "custom_a", "title": "周末", "options": [{"id": "x", "text": "爬山"}]}
              ]
            }
        """.trimIndent()

        val decoded = DecisionArchiveCodec.decode(raw)

        assertNotNull(decoded)
        assertEquals(1, decoded?.size)
        assertEquals(DecisionWeight.MIN, decoded?.first()?.options?.first()?.weight)
    }

    @Test
    fun decode_returnsNullForBrokenPayload() {
        assertNull(DecisionArchiveCodec.decode("not a json"))
        assertNull(DecisionArchiveCodec.decode(""))
    }
}
