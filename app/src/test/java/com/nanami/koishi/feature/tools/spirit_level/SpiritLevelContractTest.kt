package com.nanami.koishi.feature.tools.spirit_level

import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.model.ToolCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SpiritLevelContractTest {

    @Test
    fun `registers tool in device category`() {
        val tool = InMemoryToolRepository().getToolById("spirit_level")

        assertNotNull(tool)
        assertEquals(ToolCategory.DEVICE_APPS, tool?.category)
    }

    @Test
    fun `status maps calibrated frame bands`() {
        assertEquals(LevelStatus.LEVEL, SpiritLevelUiState(frame = frame(0.4, -0.4)).status)
        assertEquals(LevelStatus.NEAR, SpiritLevelUiState(frame = frame(1.0, 1.0)).status)
        assertEquals(LevelStatus.OFF, SpiritLevelUiState(frame = frame(4.0, 0.0)).status)
    }

    private fun frame(pitch: Double, roll: Double) =
        com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelFrame(
            pitchDeg = pitch,
            rollDeg = roll
        )
}
