package com.nanami.koishi.feature.tools.text_apps.diff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffEngineTest {

    @Test
    fun detectsInsertionsAndDeletions() {
        val result = DiffEngine.diff("one\ntwo\nthree", "one\nthree\nfour").getOrThrow()

        assertEquals(
            listOf(DiffLineKind.EQUAL, DiffLineKind.DELETE, DiffLineKind.EQUAL, DiffLineKind.INSERT),
            result.lines.map { it.kind }
        )
        assertEquals(1, result.additions)
        assertEquals(1, result.deletions)
    }

    @Test
    fun canIgnoreWhitespaceAndCase() {
        val result = DiffEngine.diff(
            oldText = "Hello World",
            newText = "  hello   world ",
            ignoreCase = true,
            ignoreWhitespace = true
        ).getOrThrow()

        assertEquals(listOf(DiffLineKind.EQUAL), result.lines.map { it.kind })
    }

    @Test
    fun rejectsOversizedInput() {
        val oldText = (1..(MAX_DIFF_LINES + 1)).joinToString("\n")
        val result = DiffEngine.diff(oldText, "short")

        assertTrue(result.exceptionOrNull() is DiffLimitExceededException)
    }
}
