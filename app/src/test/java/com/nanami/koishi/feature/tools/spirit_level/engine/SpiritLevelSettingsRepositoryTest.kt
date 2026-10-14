package com.nanami.koishi.feature.tools.spirit_level.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpiritLevelSettingsRepositoryTest {

    private val repository = SpiritLevelSettingsRepository(FakeToolStorageDao())

    @Test
    fun `uses surface mode without calibration by default`() = runTest {
        val settings = repository.currentData()

        assertEquals(LevelMode.SURFACE, settings.mode)
        assertEquals(0.0, settings.pitchOffsetDeg, 0.0)
        assertEquals(0.0, settings.rollOffsetDeg, 0.0)
        assertEquals(0.0, settings.tiltOffsetDeg, 0.0)
    }

    @Test
    fun `persists mode and calibration`() = runTest {
        repository.updateMode(LevelMode.INCLINE)
        repository.updateCalibration(
            SpiritLevelFrame(pitchDeg = 1.2, rollDeg = -0.8, tiltDeg = 3.4)
        )

        val settings = repository.currentData()

        assertEquals(LevelMode.INCLINE, settings.mode)
        assertEquals(1.2, settings.pitchOffsetDeg, 0.0)
        assertEquals(-0.8, settings.rollOffsetDeg, 0.0)
        assertEquals(3.4, settings.tiltOffsetDeg, 0.0)
    }

    @Test
    fun `resets calibration without changing mode`() = runTest {
        repository.updateMode(LevelMode.INCLINE)
        repository.updateCalibration(SpiritLevelFrame(1.0, 2.0, 3.0))

        val settings = repository.resetCalibration()

        assertEquals(LevelMode.INCLINE, settings.mode)
        assertEquals(0.0, settings.pitchOffsetDeg, 0.0)
        assertEquals(0.0, settings.rollOffsetDeg, 0.0)
        assertEquals(0.0, settings.tiltOffsetDeg, 0.0)
    }
}
