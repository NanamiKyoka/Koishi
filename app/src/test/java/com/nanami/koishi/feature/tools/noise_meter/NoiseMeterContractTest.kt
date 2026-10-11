package com.nanami.koishi.feature.tools.noise_meter

import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseMeterContractTest {

    @Test
    fun `gauge fraction maps level into the display range`() {
        assertEquals(0.5f, NoiseMeterUiState(levelDb = 60.0).gaugeFraction, 1e-6f)
    }

    @Test
    fun `gauge fraction clamps out of range levels`() {
        assertEquals(0f, NoiseMeterUiState(levelDb = -20.0).gaugeFraction, 1e-6f)
        assertEquals(1f, NoiseMeterUiState(levelDb = 500.0).gaugeFraction, 1e-6f)
    }

    @Test
    fun `noise levels map to expected bands`() {
        assertEquals(NoiseLevel.VERY_QUIET, NoiseLevel.fromDb(0.0))
        assertEquals(NoiseLevel.VERY_QUIET, NoiseLevel.fromDb(29.9))
        assertEquals(NoiseLevel.QUIET, NoiseLevel.fromDb(30.0))
        assertEquals(NoiseLevel.MODERATE, NoiseLevel.fromDb(40.0))
        assertEquals(NoiseLevel.NOISY, NoiseLevel.fromDb(55.0))
        assertEquals(NoiseLevel.VERY_NOISY, NoiseLevel.fromDb(70.0))
        assertEquals(NoiseLevel.DANGEROUS, NoiseLevel.fromDb(85.0))
        assertEquals(NoiseLevel.EXTREME, NoiseLevel.fromDb(100.0))
        assertEquals(NoiseLevel.EXTREME, NoiseLevel.fromDb(118.0))
    }
}