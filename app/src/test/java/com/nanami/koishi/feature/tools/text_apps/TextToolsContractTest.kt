package com.nanami.koishi.feature.tools.text_apps

import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.model.ToolCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TextToolsContractTest {

    @Test
    fun registersAllTextToolsInTextCategory() {
        val repository = InMemoryToolRepository()
        val expectedIds = setOf(
            "json_formatter",
            "text_encoding",
            "text_hash",
            "regex_tool",
            "text_process",
            "text_diff"
        )

        val actualIds = runBlocking {
            repository.availableTools.first()
                .filter { it.category == ToolCategory.TEXT_APPS }
                .map { it.id }
                .toSet()
        }

        assertEquals(expectedIds, actualIds)
    }
}
