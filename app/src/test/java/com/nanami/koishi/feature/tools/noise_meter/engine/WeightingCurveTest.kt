package com.nanami.koishi.feature.tools.noise_meter.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightingCurveTest {

    @Test
    fun `a weighting matches reference table`() {
        val reference = mapOf(
            20.0 to -50.4,
            100.0 to -19.1,
            500.0 to -3.2,
            1000.0 to 0.0,
            10000.0 to -2.5
        )
        reference.forEach { (frequency, expected) ->
            assertEquals(
                "frequency=$frequency",
                expected,
                WeightingCurve.gainDb(FrequencyWeighting.A, frequency),
                0.3
            )
        }
    }

    @Test
    fun `c weighting matches reference table`() {
        val reference = mapOf(
            100.0 to -0.3,
            1000.0 to 0.0,
            10000.0 to -4.4
        )
        reference.forEach { (frequency, expected) ->
            assertEquals(
                "frequency=$frequency",
                expected,
                WeightingCurve.gainDb(FrequencyWeighting.C, frequency),
                0.2
            )
        }
    }

    @Test
    fun `z weighting is flat`() {
        listOf(20.0, 200.0, 2000.0, 20000.0).forEach { frequency ->
            assertEquals(0.0, WeightingCurve.gainDb(FrequencyWeighting.Z, frequency), 0.0)
        }
    }

    @Test
    fun `non positive frequency is muted`() {
        assertTrue(WeightingCurve.gainDb(FrequencyWeighting.A, 0.0).isInfinite())
    }
}