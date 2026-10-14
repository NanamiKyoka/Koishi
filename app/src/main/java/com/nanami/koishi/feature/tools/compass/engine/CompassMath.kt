package com.nanami.koishi.feature.tools.compass.engine

import android.view.Surface
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal object CompassMath {
    fun normalizeDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

    fun applyDisplayRotation(headingDeg: Double, rotation: Int): Double {
        val offset = when (rotation) {
            Surface.ROTATION_90 -> 90.0
            Surface.ROTATION_180 -> 180.0
            Surface.ROTATION_270 -> 270.0
            else -> 0.0
        }
        return normalizeDegrees(headingDeg + offset)
    }

    fun smoothHeading(previousDeg: Double?, nextDeg: Double, factor: Double = 0.18): Double {
        val next = normalizeDegrees(nextDeg)
        if (previousDeg == null) return next

        val previousRad = Math.toRadians(normalizeDegrees(previousDeg))
        val nextRad = Math.toRadians(next)
        val weight = factor.coerceIn(0.0, 1.0)
        val sinValue = sin(previousRad) * (1.0 - weight) + sin(nextRad) * weight
        val cosValue = cos(previousRad) * (1.0 - weight) + cos(nextRad) * weight
        return normalizeDegrees(Math.toDegrees(atan2(sinValue, cosValue)))
    }
}
