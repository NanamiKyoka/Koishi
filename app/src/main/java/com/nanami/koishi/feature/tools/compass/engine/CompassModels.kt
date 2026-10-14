package com.nanami.koishi.feature.tools.compass.engine

import com.nanami.koishi.core.sensor.SensorAccuracy

data class CompassFrame(
    val headingDeg: Double = 0.0,
    val pitchDeg: Double = 0.0,
    val rollDeg: Double = 0.0,
    val accuracy: SensorAccuracy = SensorAccuracy.UNKNOWN
) {
    val tiltDeg: Double
        get() = kotlin.math.hypot(pitchDeg, rollDeg)
}
