package com.nanami.koishi.feature.tools.date_time_calculator.engine

import kotlinx.serialization.Serializable

@Serializable
data class DateTimeCalculatorData(
    val mode: String = "interval",
    val interval: IntervalDraft = IntervalDraft(),
    val arithmetic: ArithmeticDraft = ArithmeticDraft(),
    val age: AgeDraft = AgeDraft(),
    val timezone: TimeZoneDraft = TimeZoneDraft()
)

@Serializable
data class IntervalDraft(
    val startDate: String = "",
    val endDate: String = "",
    val includeTime: Boolean = false,
    val startTime: String = "00:00",
    val endTime: String = "23:59"
)

@Serializable
data class ArithmeticDraft(
    val baseDate: String = "",
    val baseTime: String = "00:00",
    val operation: String = "add",
    val years: String = "0",
    val months: String = "0",
    val weeks: String = "0",
    val days: String = "0",
    val hours: String = "0",
    val minutes: String = "0"
)

@Serializable
data class AgeDraft(
    val birthDate: String = "",
    val targetDate: String = ""
)

@Serializable
data class TimeZoneDraft(
    val sourceZoneId: String = "",
    val targetZoneId: String = "UTC",
    val date: String = "",
    val time: String = "00:00"
)