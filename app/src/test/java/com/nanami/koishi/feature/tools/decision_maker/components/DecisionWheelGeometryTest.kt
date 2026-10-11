package com.nanami.koishi.feature.tools.decision_maker.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DecisionWheelGeometryTest {

    @Test
    fun randomizedLandingAngle_staysInsideSafePortion() {
        val start = -90f
        val sweep = 90f
        val low = randomizedLandingAngle(start, sweep, 0f)
        val high = randomizedLandingAngle(start, sweep, 1f)

        assertEquals(start + sweep * 0.15f, low, 0.001f)
        assertEquals(start + sweep * 0.85f, high, 0.001f)
        assertTrue(low > start)
        assertTrue(high < start + sweep)
        assertNotEquals(start + sweep / 2f, low)
    }

    @Test
    fun randomizedLandingAngle_variesWithRandomValue() {
        val angles = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            .map { randomizedLandingAngle(startAngle = -90f, sweep = 72f, randomValue = it) }

        assertEquals(angles.size, angles.distinct().size)
    }

    @Test
    fun randomizedLandingAngle_usesCenterForNarrowSlices() {
        val start = 120f
        val sweep = 2f

        assertEquals(start + sweep / 2f, randomizedLandingAngle(start, sweep, 0f), 0.001f)
        assertEquals(start + sweep / 2f, randomizedLandingAngle(start, sweep, 1f), 0.001f)
    }

    @Test
    fun nextTarget_alignsPointerWithLandingAngle() {
        val start = -90f
        val sweep = 120f
        val landingAngle = randomizedLandingAngle(start, sweep, 0.7f)
        val targetRotation = nextTarget(currentRotation = 0f, desiredAngle = -90f - landingAngle)
        val pointerAngle = normalizeDegrees(-90f - targetRotation)

        assertTrue(abs(pointerAngle - normalizeDegrees(landingAngle)) < 0.001f)
        assertTrue(targetRotation > 360f * 5f)
    }

    private fun normalizeDegrees(value: Float): Float {
        val mod = value % 360f
        return if (mod < 0f) mod + 360f else mod
    }
}