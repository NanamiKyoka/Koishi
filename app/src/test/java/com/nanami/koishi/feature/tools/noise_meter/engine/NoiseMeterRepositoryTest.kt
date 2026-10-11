package com.nanami.koishi.feature.tools.noise_meter.engine

import com.nanami.koishi.core.data.storage.FakeToolStorageDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseMeterRepositoryTest {

    private val dao = FakeToolStorageDao()
    private val repository = NoiseMeterRepository(dao)

    @Test
    fun `defaults to a weighting with default calibration`() = runTest {
        val data = repository.currentData()

        assertEquals(NoiseConfig.DEFAULT_CALIBRATION_OFFSET_DB, data.calibrationOffsetDb, 0.0)
        assertEquals(FrequencyWeighting.A, data.frequencyWeighting)
        assertEquals(TimeWeighting.FAST, data.timeWeighting)
    }

    @Test
    fun `settings persist across updates`() = runTest {
        repository.updateCalibrationOffset(112.5)
        repository.updateFrequencyWeighting(FrequencyWeighting.C)
        repository.updateTimeWeighting(TimeWeighting.SLOW)

        val data = repository.currentData()
        assertEquals(112.5, data.calibrationOffsetDb, 0.0)
        assertEquals(FrequencyWeighting.C, data.frequencyWeighting)
        assertEquals(TimeWeighting.SLOW, data.timeWeighting)
    }
}