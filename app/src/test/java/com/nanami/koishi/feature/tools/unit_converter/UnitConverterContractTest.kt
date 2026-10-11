package com.nanami.koishi.feature.tools.unit_converter

import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.model.ToolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UnitConverterContractTest {

    @Test
    fun registersToolInCalculationCategory() {
        val tool = InMemoryToolRepository().getToolById("unit_converter")
        assertNotNull(tool)
        assertEquals(ToolCategory.CALCULATION, tool?.category)
    }
}