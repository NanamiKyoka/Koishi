package com.nanami.koishi.feature.tools.decision_maker.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WeightedPickerTest {

    private fun option(id: String, weight: Int, text: String = id) =
        DecisionOption(id = id, text = text, weight = weight)

    @Test
    fun pick_returnsNullWhenNothingIsPlayable() {
        val options = listOf(option("a", 1, text = " "), option("b", 1, text = ""))
        assertNull(WeightedPicker.pick(options))
    }

    @Test
    fun pick_ignoresBlankOptions() {
        val options = listOf(option("blank", 10, text = "   "), option("real", 1, text = "火锅"))
        assertEquals("real", WeightedPicker.pick(options)?.id)
    }

    @Test
    fun pick_clampsWeightsIntoValidRange() {
        val options = listOf(option("low", -5), option("high", 99))
        val random = Random(7)

        val hits = (1..400).count { WeightedPicker.pick(options, random = random)?.id == "high" }
        assertTrue("clamped weight 10:1 应该约占九成样本，实际 $hits", hits in 330..398)
    }

    @Test
    fun pick_distributesSamplesAccordingToWeight() {
        val options = listOf(option("heavy", 9), option("light", 1))
        val random = Random(1234)

        val heavy = (1..1000).count { WeightedPicker.pick(options, random = random)?.id == "heavy" }
        assertTrue("weight 9 应该占到约九成样本，实际 $heavy", heavy in 860..940)
    }

    @Test
    fun sweepAngles_areProportionalAndSumToFullCircle() {
        val options = listOf(option("a", 1), option("b", 3))
        val sweeps = WeightedPicker.sweepAngles(options)

        assertEquals(2, sweeps.size)
        assertEquals(360f, sweeps.sum(), 0.001f)
        assertEquals(90f, sweeps[0], 0.001f)
        assertEquals(270f, sweeps[1], 0.001f)
    }

    @Test
    fun sweepAngles_skipBlankOptions() {
        val options = listOf(option("blank", 5, text = ""), option("real", 5, text = "烤肉"))
        val sweeps = WeightedPicker.sweepAngles(options)

        assertEquals(1, sweeps.size)
        assertEquals(360f, sweeps[0], 0.001f)
    }

    @Test
    fun pick_singleCandidateIsAlwaysReturned() {
        val options = listOf(option("only", 1, text = "麻辣烫"))
        val picked = WeightedPicker.pick(options)
        assertNotNull(picked)
        assertEquals("麻辣烫", picked?.text)
    }
}
