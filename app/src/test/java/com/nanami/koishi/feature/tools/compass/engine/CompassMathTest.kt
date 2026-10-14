package com.nanami.koishi.feature.tools.compass.engine

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassMathTest {

    @Test
    fun `normalizes heading into zero to 360 range`() {
        assertEquals(10.0, CompassMath.normalizeDegrees(370.0), 1e-6)
        assertEquals(350.0, CompassMath.normalizeDegrees(-10.0), 1e-6)
    }

    @Test
    fun `applies display rotation to heading`() {
        assertEquals(80.0, CompassMath.applyDisplayRotation(350.0, Surface.ROTATION_90), 1e-6)
        assertEquals(10.0, CompassMath.applyDisplayRotation(190.0, Surface.ROTATION_180), 1e-6)
        assertEquals(0.0, CompassMath.applyDisplayRotation(90.0, Surface.ROTATION_270), 1e-6)
    }

    @Test
    fun `smooths heading across north without wrapping back`() {
        val heading = CompassMath.smoothHeading(359.0, 1.0, 0.5)
        assertEquals(0.0, heading, 1e-6)
    }
}
