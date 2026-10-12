package com.nanami.koishi.feature.tools.text_apps.text_process

import org.junit.Assert.assertEquals
import org.junit.Test

class TextProcessEngineTest {

    @Test
    fun caseModesHandleWordsAndSentences() {
        assertEquals("hello world", TextProcessEngine.transformCase("Hello World", CaseMode.LOWERCASE))
        assertEquals("HELLO WORLD", TextProcessEngine.transformCase("Hello World", CaseMode.UPPERCASE))
        assertEquals("Hello World", TextProcessEngine.transformCase("hello world", CaseMode.TITLE))
        assertEquals("Hello world. Next line!", TextProcessEngine.transformCase("hello world. next line!", CaseMode.SENTENCE))
        assertEquals("hELLO", TextProcessEngine.transformCase("Hello", CaseMode.SWAP))
    }

    @Test
    fun deduplicateCanIgnoreCaseAndTrim() {
        val result = TextProcessEngine.deduplicate(
            " Apple \napple\nBanana\n\n",
            TextProcessOptions(trimLines = true, removeEmpty = true, ignoreCase = true)
        )

        assertEquals("Apple\nBanana", result)
    }

    @Test
    fun naturalSortOrdersNumbersNumerically() {
        val result = TextProcessEngine.sortLines(
            "item10\nitem2\nitem1",
            SortMode.NATURAL,
            TextProcessOptions()
        )

        assertEquals("item1\nitem2\nitem10", result)
    }

    @Test
    fun statisticsCountUnicodeText() {
        val result = TextProcessEngine.statistics("恋恋 abc\n\n🙂")

        assertEquals(9, result.characters)
        assertEquals(6, result.charactersWithoutWhitespace)
        assertEquals(5, result.letters)
        assertEquals(2, result.cjkCharacters)
        assertEquals(3, result.lines)
        assertEquals(2, result.paragraphs)
        assertEquals(16, result.utf8Bytes)
    }
}
