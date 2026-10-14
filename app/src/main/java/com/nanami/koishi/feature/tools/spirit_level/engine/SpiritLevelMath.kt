package com.nanami.koishi.feature.tools.spirit_level.engine

import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

internal object SpiritLevelMath {
    fun smoothVector(previous: FloatArray?, next: FloatArray, factor: Double = 0.18): FloatArray {
        if (previous == null || previous.size < 3) return next.copyOf()
        val weight = factor.coerceIn(0.0, 1.0)
        return FloatArray(3) { index ->
            (previous[index] * (1.0 - weight) + next[index] * weight).toFloat()
        }
    }

    fun remapGravity(values: FloatArray, displayRotation: Int): Pair<Float, Float> {
        val x = values.getOrElse(0) { 0f }
        val y = values.getOrElse(1) { 0f }
        return when (displayRotation) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }
    }

    fun fromGravity(values: FloatArray, displayRotation: Int): SpiritLevelFrame {
        val z = values.getOrElse(2) { 0f }.toDouble()
        val (screenX, screenY) = remapGravity(values, displayRotation)
        val x = screenX.toDouble()
        val y = screenY.toDouble()
        val horizontal = hypot(x, y)
        val pitch = Math.toDegrees(atan2(-x, sqrt(y * y + z * z)))
        val roll = Math.toDegrees(atan2(y, z))
        val tilt = Math.toDegrees(atan2(horizontal, z))
        return SpiritLevelFrame(
            pitchDeg = pitch,
            rollDeg = roll,
            tiltDeg = tilt
        )
    }

    fun applyCalibration(frame: SpiritLevelFrame, settings: SpiritLevelSettings): SpiritLevelFrame =
        frame.copy(
            pitchDeg = frame.pitchDeg - settings.pitchOffsetDeg,
            rollDeg = frame.rollDeg - settings.rollOffsetDeg,
            tiltDeg = normalizeTilt(frame.tiltDeg - settings.tiltOffsetDeg)
        )

    fun normalizeTilt(value: Double): Double = ((value % 180.0) + 180.0) % 180.0

    fun isLevel(frame: SpiritLevelFrame): Boolean =
        abs(frame.pitchDeg) <= LEVEL_THRESHOLD_DEG && abs(frame.rollDeg) <= LEVEL_THRESHOLD_DEG

    fun isNearLevel(frame: SpiritLevelFrame): Boolean =
        abs(frame.pitchDeg) <= NEAR_THRESHOLD_DEG && abs(frame.rollDeg) <= NEAR_THRESHOLD_DEG

    const val LEVEL_THRESHOLD_DEG = 0.5
    const val NEAR_THRESHOLD_DEG = 2.0
}
