package com.nanami.koishi.feature.tools.date_time_calculator.engine

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.Year
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateTimeCalculatorEngine {

    fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()

    fun calculateDateInterval(start: LocalDate, end: LocalDate): DateIntervalResult {
        val totalDays = ChronoUnit.DAYS.between(start, end)
        val period = Period.between(start, end)
        val totalWeeks = totalDays / 7
        return DateIntervalResult(
            period = period,
            totalDays = totalDays,
            totalWeeks = totalWeeks,
            remainingDays = totalDays % 7
        )
    }

    fun calculateDateTimeInterval(
        start: LocalDateTime,
        end: LocalDateTime
    ): DateIntervalResult {
        val dateResult = calculateDateInterval(start.toLocalDate(), end.toLocalDate())
        return dateResult.copy(duration = Duration.between(start, end))
    }

    fun shift(
        date: LocalDate,
        time: LocalTime,
        operation: DateOperation,
        values: ArithmeticValues
    ): ArithmeticResult {
        val sign = if (operation == DateOperation.ADD) 1L else -1L
        val shifted = LocalDateTime.of(date, time)
            .plusYears(values.years * sign)
            .plusMonths(values.months * sign)
            .plusWeeks(values.weeks * sign)
            .plusDays(values.days * sign)
            .plusHours(values.hours * sign)
            .plusMinutes(values.minutes * sign)
        return ArithmeticResult(
            dateTime = shifted,
            hasTimeComponent = values.hours != 0L || values.minutes != 0L
        )
    }

    fun calculateAge(birthDate: LocalDate, targetDate: LocalDate): AgeResult {
        val period = Period.between(birthDate, targetDate)
        val nextBirthday = nextBirthday(birthDate, targetDate)
        return AgeResult(
            period = period,
            totalDays = ChronoUnit.DAYS.between(birthDate, targetDate),
            nextBirthday = nextBirthday,
            daysUntilNextBirthday = ChronoUnit.DAYS.between(targetDate, nextBirthday)
        )
    }

    fun convertTimeZone(
        dateTime: LocalDateTime,
        sourceZone: ZoneId,
        targetZone: ZoneId
    ): TimeZoneConversionResult {
        val source = dateTime.atZone(sourceZone)
        val target = source.withZoneSameInstant(targetZone)
        return TimeZoneConversionResult(
            sourceDateTime = source,
            targetDateTime = target,
            offsetDifferenceSeconds = target.offset.totalSeconds.toLong() - source.offset.totalSeconds,
            dayDifference = ChronoUnit.DAYS.between(source.toLocalDate(), target.toLocalDate())
        )
    }

    fun availableZoneIds(): List<String> = ZoneId.getAvailableZoneIds().sorted()

    private fun nextBirthday(birthDate: LocalDate, targetDate: LocalDate): LocalDate {
        val thisYear = birthdayInYear(birthDate, targetDate.year)
        return if (thisYear.isBefore(targetDate)) birthdayInYear(birthDate, targetDate.year + 1) else thisYear
    }

    private fun birthdayInYear(birthDate: LocalDate, year: Int): LocalDate {
        if (birthDate.monthValue == 2 && birthDate.dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            return LocalDate.of(year, 2, 28)
        }
        return LocalDate.of(year, birthDate.monthValue, birthDate.dayOfMonth)
    }
}