package com.nanami.koishi.feature.tools.spirit_level.engine

import com.nanami.koishi.core.sensor.SensorAccuracy
import kotlinx.serialization.Serializable

@Serializable
enum class LevelMode {
    SURFACE,
    INCLINE
}

data class SpiritLevelFrame(
    val pitchDeg: Double = 0.0,
    val rollDeg: Double = 0.0,
    val tiltDeg: Double = 0.0,
    val accuracy: SensorAccuracy = SensorAccuracy.UNKNOWN
)

@Serializable
data class SpiritLevelSettings(
    val mode: LevelMode = LevelMode.SURFACE,
    val pitchOffsetDeg: Double = 0.0,
    val rollOffsetDeg: Double = 0.0,
    val tiltOffsetDeg: Double = 0.0
)
