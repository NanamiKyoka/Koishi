package com.nanami.koishi.feature.tools.date_time_calculator.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

data class DateIntervalResult(
    val period: Period,
    val totalDays: Long,
    val totalWeeks: Long,
    val remainingDays: Long,
    val duration: Duration? = null
)

data class ArithmeticResult(
    val dateTime: LocalDateTime,
    val hasTimeComponent: Boolean
)

data class AgeResult(
    val period: Period,
    val totalDays: Long,
    val nextBirthday: LocalDate,
    val daysUntilNextBirthday: Long
)

data class TimeZoneConversionResult(
    val sourceDateTime: ZonedDateTime,
    val targetDateTime: ZonedDateTime,
    val offsetDifferenceSeconds: Long,
    val dayDifference: Long
)

enum class DateOperation {
    ADD,
    SUBTRACT;
    companion object {
        fun fromId(id: String): DateOperation = if (id == "subtract") SUBTRACT else ADD
    }
}

data class ArithmeticValues(
    val years: Long = 0,
    val months: Long = 0,
    val weeks: Long = 0,
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0
)

fun ZoneId?.orSystemDefault(): ZoneId = this ?: ZoneId.systemDefault()