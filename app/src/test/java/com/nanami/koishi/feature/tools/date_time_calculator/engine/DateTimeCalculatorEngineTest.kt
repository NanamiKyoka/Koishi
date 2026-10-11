package com.nanami.koishi.feature.tools.date_time_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DateTimeCalculatorEngineTest {

    @Test
    fun calculatesDateIntervalAcrossLeapYear() {
        val result = DateTimeCalculatorEngine.calculateDateInterval(
            LocalDate.of(2020, 2, 28),
            LocalDate.of(2020, 3, 1)
        )
        assertEquals(2L, result.totalDays)
        assertEquals(0L, result.totalWeeks)
        assertEquals(2L, result.remainingDays)
        assertEquals(0, result.period.months)
        assertEquals(2, result.period.days)
    }

    @Test
    fun calculatesNegativeDateInterval() {
        val result = DateTimeCalculatorEngine.calculateDateInterval(
            LocalDate.of(2024, 1, 10),
            LocalDate.of(2024, 1, 1)
        )
        assertEquals(-9L, result.totalDays)
        assertEquals(-1L, result.totalWeeks)
        assertEquals(-2L, result.remainingDays)
    }

    @Test
    fun shiftsMonthEndAndCarriesTime() {
        val monthEnd = DateTimeCalculatorEngine.shift(
            date = LocalDate.of(2024, 1, 31),
            time = LocalTime.of(23, 30),
            operation = DateOperation.ADD,
            values = ArithmeticValues(months = 1, hours = 1)
        )
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 30), monthEnd.dateTime)
    }

    @Test
    fun calculatesAgeAndNextLeapDayBirthday() {
        val result = DateTimeCalculatorEngine.calculateAge(
            birthDate = LocalDate.of(2000, 2, 29),
            targetDate = LocalDate.of(2023, 3, 1)
        )
        assertEquals(23, result.period.years)
        assertEquals(LocalDate.of(2024, 2, 29), result.nextBirthday)
        assertEquals(365L, result.daysUntilNextBirthday)
    }

    @Test
    fun treatsBirthdayTodayAsZeroDaysRemaining() {
        val result = DateTimeCalculatorEngine.calculateAge(
            birthDate = LocalDate.of(2000, 10, 12),
            targetDate = LocalDate.of(2026, 10, 12)
        )
        assertEquals(0L, result.daysUntilNextBirthday)
    }

    @Test
    fun convertsTimeZonesAndDetectsDayChange() {
        val result = DateTimeCalculatorEngine.convertTimeZone(
            dateTime = LocalDateTime.of(2024, 1, 1, 23, 0),
            sourceZone = ZoneId.of("UTC"),
            targetZone = ZoneId.of("Asia/Shanghai")
        )
        assertEquals(LocalDateTime.of(2024, 1, 2, 7, 0), result.targetDateTime.toLocalDateTime())
        assertEquals(1L, result.dayDifference)
        assertEquals(8 * 3600L, result.offsetDifferenceSeconds)
    }

    @Test
    fun exposesTimeZoneIds() {
        val zones = DateTimeCalculatorEngine.availableZoneIds()
        assertTrue(zones.contains("UTC"))
        assertTrue(zones.contains("Asia/Shanghai"))
    }
}