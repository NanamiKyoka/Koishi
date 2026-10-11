package com.nanami.koishi.feature.tools.date_time_calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.date_time_calculator.engine.ArithmeticDraft
import com.nanami.koishi.feature.tools.date_time_calculator.engine.ArithmeticValues
import com.nanami.koishi.feature.tools.date_time_calculator.engine.AgeDraft
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateOperation
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateTimeCalculatorData
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateTimeCalculatorEngine
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateTimeCalculatorRepository
import com.nanami.koishi.feature.tools.date_time_calculator.engine.IntervalDraft
import com.nanami.koishi.feature.tools.date_time_calculator.engine.TimeZoneDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

class DateTimeCalculatorViewModel(
    application: Application,
    private val repository: DateTimeCalculatorRepository
) : AndroidViewModel(application) {

    private val today = LocalDate.now()
    private val currentTime = LocalTime.now().withSecond(0).withNano(0)
    private val systemZoneId = ZoneId.systemDefault().id

    private val _uiState = MutableStateFlow(
        DateTimeCalculatorUiState(
            intervalStartDate = today.toString(),
            intervalEndDate = today.plusDays(1).toString(),
            arithmeticBaseDate = today.toString(),
            ageBirthDate = today.minusYears(20).toString(),
            ageTargetDate = today.toString(),
            timeZoneSourceId = systemZoneId,
            timeZoneDate = today.toString(),
            timeZoneTime = currentTime.toString(),
            timeZoneIds = DateTimeCalculatorEngine.availableZoneIds()
        )
    )
    val uiState: StateFlow<DateTimeCalculatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = repository.currentData()
            val state = restore(stored)
            _uiState.value = recalculate(state)
        }
    }

    fun onEvent(event: DateTimeCalculatorUiEvent) {
        when (event) {
            is DateTimeCalculatorUiEvent.OnModeSelected -> update { it.copy(mode = event.mode) }
            is DateTimeCalculatorUiEvent.OnIntervalStartDateChange -> update { it.copy(intervalStartDate = event.value) }
            is DateTimeCalculatorUiEvent.OnIntervalEndDateChange -> update { it.copy(intervalEndDate = event.value) }
            is DateTimeCalculatorUiEvent.OnIntervalIncludeTimeChange -> update { it.copy(intervalIncludeTime = event.value) }
            is DateTimeCalculatorUiEvent.OnIntervalStartTimeChange -> update { it.copy(intervalStartTime = event.value) }
            is DateTimeCalculatorUiEvent.OnIntervalEndTimeChange -> update { it.copy(intervalEndTime = event.value) }
            is DateTimeCalculatorUiEvent.OnArithmeticBaseDateChange -> update { it.copy(arithmeticBaseDate = event.value) }
            is DateTimeCalculatorUiEvent.OnArithmeticBaseTimeChange -> update { it.copy(arithmeticBaseTime = event.value) }
            is DateTimeCalculatorUiEvent.OnArithmeticOperationChange -> update {
                it.copy(arithmeticOperation = event.operation)
            }
            is DateTimeCalculatorUiEvent.OnArithmeticValueChange -> updateArithmeticValue(event.field, event.value)
            is DateTimeCalculatorUiEvent.OnAgeBirthDateChange -> update { it.copy(ageBirthDate = event.value) }
            is DateTimeCalculatorUiEvent.OnAgeTargetDateChange -> update { it.copy(ageTargetDate = event.value) }
            is DateTimeCalculatorUiEvent.OnTimeZoneSourceChange -> update { it.copy(timeZoneSourceId = event.zoneId) }
            is DateTimeCalculatorUiEvent.OnTimeZoneTargetChange -> update { it.copy(timeZoneTargetId = event.zoneId) }
            is DateTimeCalculatorUiEvent.OnTimeZoneDateChange -> update { it.copy(timeZoneDate = event.value) }
            is DateTimeCalculatorUiEvent.OnTimeZoneTimeChange -> update { it.copy(timeZoneTime = event.value) }
            DateTimeCalculatorUiEvent.OnSwapTimeZones -> update {
                it.copy(timeZoneSourceId = it.timeZoneTargetId, timeZoneTargetId = it.timeZoneSourceId)
            }
            is DateTimeCalculatorUiEvent.OnOpenTimeZonePicker -> {
                _uiState.value = _uiState.value.copy(timeZonePickerTarget = event.target)
            }
            DateTimeCalculatorUiEvent.OnDismissTimeZonePicker -> {
                _uiState.value = _uiState.value.copy(timeZonePickerTarget = null)
            }
        }
    }

    private fun updateArithmeticValue(field: ArithmeticField, value: String) {
        val sanitized = value.filter(Char::isDigit).take(6)
        update { state ->
            when (field) {
                ArithmeticField.YEARS -> state.copy(arithmeticYears = sanitized)
                ArithmeticField.MONTHS -> state.copy(arithmeticMonths = sanitized)
                ArithmeticField.WEEKS -> state.copy(arithmeticWeeks = sanitized)
                ArithmeticField.DAYS -> state.copy(arithmeticDays = sanitized)
                ArithmeticField.HOURS -> state.copy(arithmeticHours = sanitized)
                ArithmeticField.MINUTES -> state.copy(arithmeticMinutes = sanitized)
            }
        }
    }

    private fun update(transform: (DateTimeCalculatorUiState) -> DateTimeCalculatorUiState) {
        val state = recalculate(transform(_uiState.value))
        _uiState.value = state
        viewModelScope.launch {
            repository.save(toData(state))
        }
    }

    private fun restore(stored: DateTimeCalculatorData): DateTimeCalculatorUiState {
        val baseState = _uiState.value
        return baseState.copy(
            mode = DateTimeMode.fromId(stored.mode),
            intervalStartDate = parseDateOr(stored.interval.startDate, today).toString(),
            intervalEndDate = parseDateOr(stored.interval.endDate, today.plusDays(1)).toString(),
            intervalIncludeTime = stored.interval.includeTime,
            intervalStartTime = formatTime(parseTimeOr(stored.interval.startTime, LocalTime.MIDNIGHT)),
            intervalEndTime = formatTime(parseTimeOr(stored.interval.endTime, LocalTime.of(23, 59))),
            arithmeticBaseDate = parseDateOr(stored.arithmetic.baseDate, today).toString(),
            arithmeticBaseTime = formatTime(parseTimeOr(stored.arithmetic.baseTime, LocalTime.MIDNIGHT)),
            arithmeticOperation = DateOperation.fromId(stored.arithmetic.operation),
            arithmeticYears = stored.arithmetic.years.ifBlank { "0" },
            arithmeticMonths = stored.arithmetic.months.ifBlank { "0" },
            arithmeticWeeks = stored.arithmetic.weeks.ifBlank { "0" },
            arithmeticDays = stored.arithmetic.days.ifBlank { "0" },
            arithmeticHours = stored.arithmetic.hours.ifBlank { "0" },
            arithmeticMinutes = stored.arithmetic.minutes.ifBlank { "0" },
            ageBirthDate = parseDateOr(stored.age.birthDate, today.minusYears(20)).toString(),
            ageTargetDate = parseDateOr(stored.age.targetDate, today).toString(),
            timeZoneSourceId = stored.timezone.sourceZoneId.ifBlank { systemZoneId },
            timeZoneTargetId = stored.timezone.targetZoneId.ifBlank { "UTC" },
            timeZoneDate = parseDateOr(stored.timezone.date, today).toString(),
            timeZoneTime = formatTime(parseTimeOr(stored.timezone.time, currentTime)),
            timeZoneIds = DateTimeCalculatorEngine.availableZoneIds()
        )
    }

    private fun recalculate(state: DateTimeCalculatorUiState): DateTimeCalculatorUiState {
        val cleared = state.copy(
            intervalPeriodText = "",
            intervalTotalText = "",
            intervalWarning = "",
            arithmeticDateText = "",
            arithmeticTimeText = "",
            arithmeticWeekdayText = "",
            ageText = "",
            ageTotalDaysText = "",
            ageNextBirthdayText = "",
            ageCountdownText = "",
            timeZoneResultDateText = "",
            timeZoneResultTimeText = "",
            timeZoneOffsetText = "",
            timeZoneDayChangeText = "",
            inputErrorRes = null
        )
        return when (state.mode) {
            DateTimeMode.INTERVAL -> recalculateInterval(cleared)
            DateTimeMode.ARITHMETIC -> recalculateArithmetic(cleared)
            DateTimeMode.AGE -> recalculateAge(cleared)
            DateTimeMode.TIME_ZONE -> recalculateTimeZone(cleared)
        }
    }

    private fun recalculateInterval(state: DateTimeCalculatorUiState): DateTimeCalculatorUiState {
        val startDate = DateTimeCalculatorEngine.parseDate(state.intervalStartDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_date)
        val endDate = DateTimeCalculatorEngine.parseDate(state.intervalEndDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_date)
        val startTime = DateTimeCalculatorEngine.parseTime(state.intervalStartTime)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_time)
        val endTime = DateTimeCalculatorEngine.parseTime(state.intervalEndTime)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_time)

        val result = if (state.intervalIncludeTime) {
            DateTimeCalculatorEngine.calculateDateTimeInterval(
                LocalDateTime.of(startDate, startTime),
                LocalDateTime.of(endDate, endTime)
            )
        } else {
            DateTimeCalculatorEngine.calculateDateInterval(startDate, endDate)
        }
        val period = result.period
        val totalText = if (state.intervalIncludeTime) {
            val totalMinutes = abs(result.duration?.toMinutes() ?: 0L)
            val days = totalMinutes / 1440
            val hours = (totalMinutes % 1440) / 60
            val minutes = totalMinutes % 60
            getString(
                R.string.date_time_calculator_interval_total_time,
                days,
                hours,
                minutes
            )
        } else {
            getString(
                R.string.date_time_calculator_interval_total_days,
                abs(result.totalDays),
                abs(result.totalWeeks),
                abs(result.remainingDays)
            )
        }
        return state.copy(
            intervalPeriodText = getString(
                R.string.date_time_calculator_interval_period,
                period.years,
                period.months,
                period.days
            ),
            intervalTotalText = totalText,
            intervalWarning = if (endDate.isBefore(startDate)) {
                getString(R.string.date_time_calculator_interval_negative)
            } else {
                ""
            }
        )
    }

    private fun recalculateArithmetic(state: DateTimeCalculatorUiState): DateTimeCalculatorUiState {
        val baseDate = DateTimeCalculatorEngine.parseDate(state.arithmeticBaseDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_date)
        val baseTime = DateTimeCalculatorEngine.parseTime(state.arithmeticBaseTime)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_time)
        val values = ArithmeticValues(
            years = state.arithmeticYears.toLongOrNull() ?: 0,
            months = state.arithmeticMonths.toLongOrNull() ?: 0,
            weeks = state.arithmeticWeeks.toLongOrNull() ?: 0,
            days = state.arithmeticDays.toLongOrNull() ?: 0,
            hours = state.arithmeticHours.toLongOrNull() ?: 0,
            minutes = state.arithmeticMinutes.toLongOrNull() ?: 0
        )
        val result = DateTimeCalculatorEngine.shift(
            date = baseDate,
            time = baseTime,
            operation = state.arithmeticOperation,
            values = values
        )
        val showTime = result.hasTimeComponent || baseTime != LocalTime.MIDNIGHT
        return state.copy(
            arithmeticDateText = formatDate(result.dateTime.toLocalDate()),
            arithmeticTimeText = if (showTime) formatTime(result.dateTime.toLocalTime()) else "",
            arithmeticWeekdayText = getString(
                R.string.date_time_calculator_arithmetic_weekday,
                formatWeekday(result.dateTime.toLocalDate())
            )
        )
    }

    private fun recalculateAge(state: DateTimeCalculatorUiState): DateTimeCalculatorUiState {
        val birthDate = DateTimeCalculatorEngine.parseDate(state.ageBirthDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_date)
        val targetDate = DateTimeCalculatorEngine.parseDate(state.ageTargetDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_invalid_date)
        if (birthDate.isAfter(targetDate)) {
            return state.copy(inputErrorRes = R.string.date_time_calculator_birth_after_target)
        }
        val result = DateTimeCalculatorEngine.calculateAge(birthDate, targetDate)
        val countdown = if (result.daysUntilNextBirthday == 0L) {
            getString(R.string.date_time_calculator_birthday_today)
        } else {
            getString(
                R.string.date_time_calculator_days_until_birthday,
                result.daysUntilNextBirthday
            )
        }
        return state.copy(
            ageText = getString(
                R.string.date_time_calculator_age_result,
                result.period.years,
                result.period.months,
                result.period.days
            ),
            ageTotalDaysText = getString(
                R.string.date_time_calculator_age_total_days,
                result.totalDays
            ),
            ageNextBirthdayText = getString(
                R.string.date_time_calculator_next_birthday,
                formatDate(result.nextBirthday)
            ),
            ageCountdownText = countdown
        )
    }

    private fun recalculateTimeZone(state: DateTimeCalculatorUiState): DateTimeCalculatorUiState {
        val date = DateTimeCalculatorEngine.parseDate(state.timeZoneDate)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_timezone_invalid)
        val time = DateTimeCalculatorEngine.parseTime(state.timeZoneTime)
            ?: return state.copy(inputErrorRes = R.string.date_time_calculator_timezone_invalid)
        val sourceZone = runCatching { ZoneId.of(state.timeZoneSourceId) }
            .getOrElse { ZoneId.systemDefault() }
        val targetZone = runCatching { ZoneId.of(state.timeZoneTargetId) }
            .getOrElse { ZoneOffset.UTC }
        val result = DateTimeCalculatorEngine.convertTimeZone(
            dateTime = LocalDateTime.of(date, time),
            sourceZone = sourceZone,
            targetZone = targetZone
        )
        val dayChangeText = when (result.dayDifference) {
            0L -> getString(R.string.date_time_calculator_timezone_same_day)
            1L -> getString(R.string.date_time_calculator_timezone_next_day)
            -1L -> getString(R.string.date_time_calculator_timezone_previous_day)
            else -> getString(
                R.string.date_time_calculator_timezone_day_change,
                result.dayDifference
            )
        }
        return state.copy(
            timeZoneResultDateText = formatDate(result.targetDateTime.toLocalDate()),
            timeZoneResultTimeText = formatTime(result.targetDateTime.toLocalTime()),
            timeZoneOffsetText = getString(
                R.string.date_time_calculator_timezone_offset,
                formatOffset(result.offsetDifferenceSeconds)
            ),
            timeZoneDayChangeText = dayChangeText
        )
    }

    private fun toData(state: DateTimeCalculatorUiState): DateTimeCalculatorData =
        DateTimeCalculatorData(
            mode = state.mode.id,
            interval = IntervalDraft(
                startDate = state.intervalStartDate,
                endDate = state.intervalEndDate,
                includeTime = state.intervalIncludeTime,
                startTime = state.intervalStartTime,
                endTime = state.intervalEndTime
            ),
            arithmetic = ArithmeticDraft(
                baseDate = state.arithmeticBaseDate,
                baseTime = state.arithmeticBaseTime,
                operation = if (state.arithmeticOperation == DateOperation.ADD) "add" else "subtract",
                years = state.arithmeticYears,
                months = state.arithmeticMonths,
                weeks = state.arithmeticWeeks,
                days = state.arithmeticDays,
                hours = state.arithmeticHours,
                minutes = state.arithmeticMinutes
            ),
            age = AgeDraft(
                birthDate = state.ageBirthDate,
                targetDate = state.ageTargetDate
            ),
            timezone = TimeZoneDraft(
                sourceZoneId = state.timeZoneSourceId,
                targetZoneId = state.timeZoneTargetId,
                date = state.timeZoneDate,
                time = state.timeZoneTime
            )
        )

    private fun parseDateOr(value: String, fallback: LocalDate): LocalDate =
        DateTimeCalculatorEngine.parseDate(value) ?: fallback

    private fun parseTimeOr(value: String, fallback: LocalTime): LocalTime =
        DateTimeCalculatorEngine.parseTime(value) ?: fallback

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()))

    private fun formatWeekday(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE", currentLocale()))

    private fun formatTime(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun formatOffset(seconds: Long): String {
        val id = ZoneOffset.ofTotalSeconds(seconds.toInt()).id
        return if (id == "Z") "+00:00" else id
    }

    private fun currentLocale(): Locale =
        getApplication<Application>().resources.configuration.locales[0] ?: Locale.getDefault()

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)
}