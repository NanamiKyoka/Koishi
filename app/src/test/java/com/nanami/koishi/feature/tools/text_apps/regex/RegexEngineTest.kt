package com.nanami.koishi.feature.tools.text_apps.regex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexEngineTest {

    @Test
    fun extractIncludesCaptureGroups() {
        val result = RegexEngine.extract(
            input = "name: Alice, name: Bob",
            pattern = """name: (\w+)"""
        ).getOrThrow()

        assertEquals(2, result.matchCount)
        assertEquals("Alice", result.matches[0].groups[1].value)
        assertEquals("Bob", result.matches[1].groups[1].value)
    }

    @Test
    fun replaceSupportsCaptureGroupReferences() {
        val result = RegexEngine.replace(
            input = "name: Alice, name: Bob",
            pattern = """name: (\w+)""",
            replacement = "user=$1"
        ).getOrThrow()

        assertEquals("user=Alice, user=Bob", result)
    }

    @Test
    fun optionsControlCaseAndMultilineMatching() {
        val options = RegexOptions(ignoreCase = true, multiline = true)
        val result = RegexEngine.extract(
            input = "First\nsecond",
            pattern = """^[a-z]+$""",
            options = options
        ).getOrThrow()

        assertEquals(2, result.matchCount)
    }

    @Test
    fun invalidPatternReturnsFailure() {
        assertTrue(RegexEngine.extract("text", "(").isFailure)
    }
}
