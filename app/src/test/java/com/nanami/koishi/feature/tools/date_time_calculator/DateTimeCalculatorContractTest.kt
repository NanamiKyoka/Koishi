package com.nanami.koishi.feature.tools.date_time_calculator

import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.model.ToolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DateTimeCalculatorContractTest {

    @Test
    fun registersToolInCalculationCategory() {
        val tool = InMemoryToolRepository().getToolById("date_time_calculator")
        assertNotNull(tool)
        assertEquals(ToolCategory.CALCULATION, tool?.category)
    }
}