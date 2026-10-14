package com.nanami.koishi.feature.tools.spirit_level.engine

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpiritLevelMathTest {

    @Test
    fun `flat phone has zero pitch roll and tilt`() {
        val frame = SpiritLevelMath.fromGravity(floatArrayOf(0f, 0f, 9.81f), Surface.ROTATION_0)

        assertEquals(0.0, frame.pitchDeg, 1e-6)
        assertEquals(0.0, frame.rollDeg, 1e-6)
        assertEquals(0.0, frame.tiltDeg, 1e-6)
    }

    @Test
    fun `remaps gravity for display rotation`() {
        val remapped = SpiritLevelMath.remapGravity(floatArrayOf(1f, 2f, 3f), Surface.ROTATION_90)

        assertEquals(-2f, remapped.first, 1e-6f)
        assertEquals(1f, remapped.second, 1e-6f)
    }

    @Test
    fun `applies calibration offsets`() {
        val frame = SpiritLevelFrame(pitchDeg = 3.0, rollDeg = -2.0, tiltDeg = 4.0)
        val settings = SpiritLevelSettings(
            pitchOffsetDeg = 3.0,
            rollOffsetDeg = -2.0,
            tiltOffsetDeg = 4.0
        )

        val calibrated = SpiritLevelMath.applyCalibration(frame, settings)

        assertEquals(0.0, calibrated.pitchDeg, 1e-6)
        assertEquals(0.0, calibrated.rollDeg, 1e-6)
        assertEquals(0.0, calibrated.tiltDeg, 1e-6)
    }

    @Test
    fun `level thresholds match display bands`() {
        assertTrue(SpiritLevelMath.isLevel(SpiritLevelFrame(pitchDeg = 0.5, rollDeg = -0.5)))
        assertFalse(SpiritLevelMath.isLevel(SpiritLevelFrame(pitchDeg = 0.6, rollDeg = 0.0)))
        assertTrue(SpiritLevelMath.isNearLevel(SpiritLevelFrame(pitchDeg = 2.0, rollDeg = -2.0)))
        assertFalse(SpiritLevelMath.isNearLevel(SpiritLevelFrame(pitchDeg = 2.1, rollDeg = 0.0)))
    }
}
