package com.nanami.koishi.feature.tools.noise_meter.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class NoiseMeterRepository(
    dao: ToolStorageDao
) : BaseToolRepository<NoiseMeterData>(
    toolId = TOOL_ID,
    serializer = NoiseMeterData.serializer(),
    dao = dao,
    defaultData = NoiseMeterData()
) {

    suspend fun updateCalibrationOffset(offsetDb: Double): NoiseMeterData = updateData { current ->
        current.copy(calibrationOffsetDb = offsetDb)
    }

    suspend fun updateFrequencyWeighting(weighting: FrequencyWeighting): NoiseMeterData = updateData { current ->
        current.copy(frequencyWeighting = weighting)
    }

    suspend fun updateTimeWeighting(weighting: TimeWeighting): NoiseMeterData = updateData { current ->
        current.copy(timeWeighting = weighting)
    }

    companion object {
        const val TOOL_ID = "noise_meter"
    }
}