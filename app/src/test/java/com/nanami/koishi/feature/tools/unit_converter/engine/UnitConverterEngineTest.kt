package com.nanami.koishi.feature.tools.unit_converter.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class UnitConverterEngineTest {

    @Test
    fun convertsLengthAndSpeed() {
        val miles = UnitConverterEngine.convert(
            BigDecimal.ONE,
            UnitCategory.LENGTH,
            "mile",
            "kilometer"
        )
        assertNotNull(miles)
        assertEquals(BigDecimal("1.609344"), miles!!.setScale(6))

        val metersPerSecond = UnitConverterEngine.convert(
            BigDecimal("36"),
            UnitCategory.SPEED,
            "kilometer_per_hour",
            "meter_per_second"
        )
        assertNotNull(metersPerSecond)
        assertEquals(BigDecimal("10.000"), metersPerSecond!!.setScale(3))
    }

    @Test
    fun convertsTemperatureWithOffsets() {
        val freezing = UnitConverterEngine.convert(
            BigDecimal.ZERO,
            UnitCategory.TEMPERATURE,
            "celsius",
            "fahrenheit"
        )
        assertNotNull(freezing)
        assertEquals(BigDecimal("32.000000"), freezing!!.setScale(6))

        val boiling = UnitConverterEngine.convert(
            BigDecimal("212"),
            UnitCategory.TEMPERATURE,
            "fahrenheit",
            "celsius"
        )
        assertNotNull(boiling)
        assertEquals(BigDecimal("100.000000"), boiling!!.setScale(6))
    }

    @Test
    fun distinguishesDecimalAndBinaryDataUnits() {
        val decimal = UnitConverterEngine.convert(
            BigDecimal.ONE,
            UnitCategory.DATA,
            "gigabyte",
            "megabyte"
        )
        assertEquals(BigDecimal("1000.000"), decimal!!.setScale(3))

        val binary = UnitConverterEngine.convert(
            BigDecimal.ONE,
            UnitCategory.DATA,
            "gibibyte",
            "mebibyte"
        )
        assertEquals(BigDecimal("1024.000"), binary!!.setScale(3))
    }

    @Test
    fun roundTripKeepsAReasonablePrecision() {
        val converted = UnitConverterEngine.convert(
            BigDecimal("123.456789"),
            UnitCategory.LENGTH,
            "mile",
            "nanometer"
        )
        val restored = UnitConverterEngine.convert(
            converted!!,
            UnitCategory.LENGTH,
            "nanometer",
            "mile"
        )
        assertEquals(0, restored!!.compareTo(BigDecimal("123.456789")))
    }

    @Test
    fun parsesAndFormatsValues() {
        assertNull(UnitConverterEngine.parseInput("not a number"))
        assertEquals(BigDecimal("1234.5"), UnitConverterEngine.parseInput("1,234.5"))
        assertEquals("0.001", UnitConverterEngine.format(BigDecimal("0.001000")))
        assertEquals("1000", UnitConverterEngine.format(BigDecimal("1E+3")))
    }
}