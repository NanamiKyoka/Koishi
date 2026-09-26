package com.nanami.koishi.feature.tools.bmi_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BmiCalculatorTest {

    @Test
    fun `bmi is weight over squared height`() {
        assertEquals(22.49, BmiCalculator.calculateBmi(170.0, 65.0), 0.01)
        assertEquals(24.22, BmiCalculator.calculateBmi(180.0, 78.5), 0.01)
    }

    @Test
    fun `invalid inputs produce zero bmi`() {
        assertEquals(0.0, BmiCalculator.calculateBmi(0.0, 60.0), 0.0)
        assertEquals(0.0, BmiCalculator.calculateBmi(170.0, -1.0), 0.0)
    }

    @Test
    fun `category boundaries follow the chinese standard`() {
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCalculator.categoryOf(18.49))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categoryOf(18.5))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categoryOf(23.99))
        assertEquals(BmiCategory.OVERWEIGHT, BmiCalculator.categoryOf(24.0))
        assertEquals(BmiCategory.OVERWEIGHT, BmiCalculator.categoryOf(27.99))
        assertEquals(BmiCategory.OBESE, BmiCalculator.categoryOf(28.0))
    }

    @Test
    fun `healthy weight range derives from body height`() {
        val range = BmiCalculator.healthyWeightRange(170.0)

        assertEquals(53.47, range.start, 0.01)
        assertEquals(69.36, range.endInclusive, 0.01)
    }

    @Test
    fun `deviation is signed against the healthy range`() {
        assertEquals(0.0, BmiCalculator.healthyWeightDeviation(170.0, 65.0), 0.01)
        assertEquals(-3.47, BmiCalculator.healthyWeightDeviation(170.0, 50.0), 0.01)
        assertEquals(5.64, BmiCalculator.healthyWeightDeviation(170.0, 75.0), 0.01)
    }

    @Test
    fun `gauge fraction spreads categories across equal quarters`() {
        assertEquals(0.0f, BmiCalculator.gaugeFraction(0.0), 0.0001f)
        assertEquals(0.125f, BmiCalculator.gaugeFraction(9.25), 0.0001f)
        assertEquals(0.25f, BmiCalculator.gaugeFraction(18.5), 0.0001f)
        assertEquals(0.5f, BmiCalculator.gaugeFraction(24.0), 0.0001f)
        assertEquals(0.75f, BmiCalculator.gaugeFraction(28.0), 0.0001f)
        assertEquals(1.0f, BmiCalculator.gaugeFraction(40.0), 0.0001f)
    }

    @Test
    fun `gauge fraction never leaves the track`() {
        val fractions = listOf(0.0, 3.0, 18.5, 24.0, 28.0, 40.0, 60.0)
            .map { BmiCalculator.gaugeFraction(it) }

        assertTrue(fractions.all { it in 0.0f..1.0f })
        assertEquals(0.0f, BmiCalculator.gaugeFraction(0.0), 0.0001f)
        assertEquals(1.0f, BmiCalculator.gaugeFraction(60.0), 0.0001f)
    }

    @Test
    fun `height and weight validity uses plausible bounds`() {
        assertTrue(BmiCalculator.isValidHeight(50.0))
        assertTrue(BmiCalculator.isValidHeight(250.0))
        assertFalse(BmiCalculator.isValidHeight(49.9))
        assertFalse(BmiCalculator.isValidHeight(250.1))

        assertTrue(BmiCalculator.isValidWeight(10.0))
        assertTrue(BmiCalculator.isValidWeight(500.0))
        assertFalse(BmiCalculator.isValidWeight(9.9))
        assertFalse(BmiCalculator.isValidWeight(500.1))
    }

    @Test
    fun `value formatting drops trailing zeros`() {
        assertEquals("22.5", BmiCalculator.formatBmi(22.46))
        assertEquals("170", BmiCalculator.formatValue(170.0))
        assertEquals("64.5", BmiCalculator.formatValue(64.5))
    }

    @Test
    fun `record derives bmi and category from stored measurements`() {
        val record = BmiRecord(id = "a", timestamp = 1L, heightCm = 170.0, weightKg = 65.0)

        assertEquals(22.49, record.bmi, 0.01)
        assertEquals(BmiCategory.NORMAL, record.category)
    }
}
