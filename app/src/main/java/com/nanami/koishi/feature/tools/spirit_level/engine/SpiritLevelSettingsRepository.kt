package com.nanami.koishi.feature.tools.spirit_level.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class SpiritLevelSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<SpiritLevelSettings>(
    toolId = TOOL_ID,
    serializer = SpiritLevelSettings.serializer(),
    dao = dao,
    defaultData = SpiritLevelSettings()
) {

    suspend fun updateMode(mode: LevelMode): SpiritLevelSettings = updateData { current ->
        current.copy(mode = mode)
    }

    suspend fun updateCalibration(frame: SpiritLevelFrame): SpiritLevelSettings = updateData { current ->
        current.copy(
            pitchOffsetDeg = frame.pitchDeg.coerceIn(-180.0, 180.0),
            rollOffsetDeg = frame.rollDeg.coerceIn(-180.0, 180.0),
            tiltOffsetDeg = frame.tiltDeg.coerceIn(0.0, 180.0)
        )
    }

    suspend fun resetCalibration(): SpiritLevelSettings = updateData { current ->
        current.copy(
            pitchOffsetDeg = 0.0,
            rollOffsetDeg = 0.0,
            tiltOffsetDeg = 0.0
        )
    }

    companion object {
        const val TOOL_ID = "spirit_level"
    }
}
