package com.nanami.koishi.feature.tools.date_time_calculator

import androidx.annotation.StringRes
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateOperation

enum class DateTimeMode(
    val id: String,
    @StringRes val labelRes: Int
) {
    INTERVAL("interval", R.string.date_time_calculator_mode_interval),
    ARITHMETIC("arithmetic", R.string.date_time_calculator_mode_arithmetic),
    AGE("age", R.string.date_time_calculator_mode_age),
    TIME_ZONE("timezone", R.string.date_time_calculator_mode_timezone);

    companion object {
        fun fromId(id: String): DateTimeMode =
            entries.firstOrNull { it.id == id } ?: INTERVAL
    }
}

enum class ArithmeticField {
    YEARS,
    MONTHS,
    WEEKS,
    DAYS,
    HOURS,
    MINUTES
}

enum class TimeZoneTarget {
    SOURCE,
    TARGET
}

data class DateTimeCalculatorUiState(
    val mode: DateTimeMode = DateTimeMode.INTERVAL,
    val intervalStartDate: String = "",
    val intervalEndDate: String = "",
    val intervalIncludeTime: Boolean = false,
    val intervalStartTime: String = "00:00",
    val intervalEndTime: String = "23:59",
    val intervalPeriodText: String = "",
    val intervalTotalText: String = "",
    val intervalWarning: String = "",
    val arithmeticBaseDate: String = "",
    val arithmeticBaseTime: String = "00:00",
    val arithmeticOperation: DateOperation = DateOperation.ADD,
    val arithmeticYears: String = "0",
    val arithmeticMonths: String = "0",
    val arithmeticWeeks: String = "0",
    val arithmeticDays: String = "0",
    val arithmeticHours: String = "0",
    val arithmeticMinutes: String = "0",
    val arithmeticDateText: String = "",
    val arithmeticTimeText: String = "",
    val arithmeticWeekdayText: String = "",
    val ageBirthDate: String = "",
    val ageTargetDate: String = "",
    val ageText: String = "",
    val ageTotalDaysText: String = "",
    val ageNextBirthdayText: String = "",
    val ageCountdownText: String = "",
    val timeZoneSourceId: String = "",
    val timeZoneTargetId: String = "UTC",
    val timeZoneDate: String = "",
    val timeZoneTime: String = "00:00",
    val timeZoneResultDateText: String = "",
    val timeZoneResultTimeText: String = "",
    val timeZoneOffsetText: String = "",
    val timeZoneDayChangeText: String = "",
    val timeZoneIds: List<String> = emptyList(),
    val timeZonePickerTarget: TimeZoneTarget? = null,
    @StringRes val inputErrorRes: Int? = null
)

sealed interface DateTimeCalculatorUiEvent {
    data class OnModeSelected(val mode: DateTimeMode) : DateTimeCalculatorUiEvent
    data class OnIntervalStartDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnIntervalEndDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnIntervalIncludeTimeChange(val value: Boolean) : DateTimeCalculatorUiEvent
    data class OnIntervalStartTimeChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnIntervalEndTimeChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnArithmeticBaseDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnArithmeticBaseTimeChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnArithmeticOperationChange(val operation: DateOperation) : DateTimeCalculatorUiEvent
    data class OnArithmeticValueChange(val field: ArithmeticField, val value: String) : DateTimeCalculatorUiEvent
    data class OnAgeBirthDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnAgeTargetDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnTimeZoneSourceChange(val zoneId: String) : DateTimeCalculatorUiEvent
    data class OnTimeZoneTargetChange(val zoneId: String) : DateTimeCalculatorUiEvent
    data class OnTimeZoneDateChange(val value: String) : DateTimeCalculatorUiEvent
    data class OnTimeZoneTimeChange(val value: String) : DateTimeCalculatorUiEvent
    data object OnSwapTimeZones : DateTimeCalculatorUiEvent
    data class OnOpenTimeZonePicker(val target: TimeZoneTarget) : DateTimeCalculatorUiEvent
    data object OnDismissTimeZonePicker : DateTimeCalculatorUiEvent
}