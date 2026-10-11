package com.nanami.koishi.feature.tools.date_time_calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateOperation
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateTimeCalculatorEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class DateTarget {
    INTERVAL_START,
    INTERVAL_END,
    ARITHMETIC_BASE,
    AGE_BIRTH,
    AGE_TARGET,
    TIME_ZONE
}

private enum class TimeTarget {
    INTERVAL_START,
    INTERVAL_END,
    ARITHMETIC_BASE,
    TIME_ZONE
}

@Composable
fun DateTimeCalculatorRoute(
    viewModel: DateTimeCalculatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DateTimeCalculatorScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeCalculatorScreen(
    uiState: DateTimeCalculatorUiState,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.date_time_calculator_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 840.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = DateTimeMode.entries.indexOf(uiState.mode),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DateTimeMode.entries.forEach { mode ->
                        Tab(
                            selected = mode == uiState.mode,
                            onClick = { onEvent(DateTimeCalculatorUiEvent.OnModeSelected(mode)) },
                            text = {
                                Text(
                                    text = stringResource(mode.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                uiState.inputErrorRes?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "content") {
                        when (uiState.mode) {
                            DateTimeMode.INTERVAL -> IntervalSection(
                                uiState = uiState,
                                onEvent = onEvent,
                                onPickDate = { dateTarget = it },
                                onPickTime = { timeTarget = it }
                            )
                            DateTimeMode.ARITHMETIC -> ArithmeticSection(
                                uiState = uiState,
                                onEvent = onEvent,
                                onPickDate = { dateTarget = it },
                                onPickTime = { timeTarget = it }
                            )
                            DateTimeMode.AGE -> AgeSection(
                                uiState = uiState,
                                onEvent = onEvent,
                                onPickDate = { dateTarget = it }
                            )
                            DateTimeMode.TIME_ZONE -> TimeZoneSection(
                                uiState = uiState,
                                onEvent = onEvent,
                                onPickDate = { dateTarget = it },
                                onPickTime = { timeTarget = it }
                            )
                        }
                    }
                }
            }
        }
    }

    dateTarget?.let { target ->
        CalculatorDatePickerDialog(
            initialDate = initialDate(target, uiState),
            onConfirm = { date ->
                onDateSelected(target, date, onEvent)
                dateTarget = null
            },
            onDismiss = { dateTarget = null }
        )
    }

    timeTarget?.let { target ->
        CalculatorTimePickerDialog(
            initialTime = initialTime(target, uiState),
            onConfirm = { time ->
                onTimeSelected(target, time, onEvent)
                timeTarget = null
            },
            onDismiss = { timeTarget = null }
        )
    }

    uiState.timeZonePickerTarget?.let { target ->
        TimeZonePickerSheet(
            timeZoneIds = uiState.timeZoneIds,
            selectedZoneId = if (target == TimeZoneTarget.SOURCE) {
                uiState.timeZoneSourceId
            } else {
                uiState.timeZoneTargetId
            },
            onSelect = { zoneId ->
                val event = if (target == TimeZoneTarget.SOURCE) {
                    DateTimeCalculatorUiEvent.OnTimeZoneSourceChange(zoneId)
                } else {
                    DateTimeCalculatorUiEvent.OnTimeZoneTargetChange(zoneId)
                }
                onEvent(event)
                onEvent(DateTimeCalculatorUiEvent.OnDismissTimeZonePicker)
            },
            onDismiss = { onEvent(DateTimeCalculatorUiEvent.OnDismissTimeZonePicker) }
        )
    }
}
@Composable
private fun IntervalSection(
    uiState: DateTimeCalculatorUiState,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    onPickDate: (DateTarget) -> Unit,
    onPickTime: (TimeTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InputCard {
            DateField(
                label = stringResource(R.string.date_time_calculator_start_date),
                value = uiState.intervalStartDate,
                onClick = { onPickDate(DateTarget.INTERVAL_START) }
            )
            DateField(
                label = stringResource(R.string.date_time_calculator_end_date),
                value = uiState.intervalEndDate,
                onClick = { onPickDate(DateTarget.INTERVAL_END) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.date_time_calculator_include_time),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = uiState.intervalIncludeTime,
                    onCheckedChange = {
                        onEvent(DateTimeCalculatorUiEvent.OnIntervalIncludeTimeChange(it))
                    }
                )
            }
            if (uiState.intervalIncludeTime) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField(
                        label = stringResource(R.string.date_time_calculator_start_date),
                        value = uiState.intervalStartTime,
                        onClick = { onPickTime(TimeTarget.INTERVAL_START) },
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        label = stringResource(R.string.date_time_calculator_end_date),
                        value = uiState.intervalEndTime,
                        onClick = { onPickTime(TimeTarget.INTERVAL_END) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.intervalPeriodText.isNotEmpty()) {
            ResultCard(title = stringResource(R.string.date_time_calculator_interval_result_title)) {
                Text(
                    text = uiState.intervalPeriodText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.intervalTotalText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.intervalWarning.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.intervalWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ArithmeticSection(
    uiState: DateTimeCalculatorUiState,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    onPickDate: (DateTarget) -> Unit,
    onPickTime: (TimeTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InputCard {
            DateField(
                label = stringResource(R.string.date_time_calculator_base_date),
                value = uiState.arithmeticBaseDate,
                onClick = { onPickDate(DateTarget.ARITHMETIC_BASE) }
            )
            TimeField(
                label = stringResource(R.string.date_time_calculator_time),
                value = uiState.arithmeticBaseTime,
                onClick = { onPickTime(TimeTarget.ARITHMETIC_BASE) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = uiState.arithmeticOperation == DateOperation.ADD,
                    onClick = {
                        onEvent(DateTimeCalculatorUiEvent.OnArithmeticOperationChange(DateOperation.ADD))
                    },
                    label = { Text(stringResource(R.string.date_time_calculator_operation_add)) }
                )
                FilterChip(
                    selected = uiState.arithmeticOperation == DateOperation.SUBTRACT,
                    onClick = {
                        onEvent(DateTimeCalculatorUiEvent.OnArithmeticOperationChange(DateOperation.SUBTRACT))
                    },
                    label = { Text(stringResource(R.string.date_time_calculator_operation_subtract)) }
                )
            }
            ArithmeticFieldRow(
                leftField = ArithmeticField.YEARS,
                leftValue = uiState.arithmeticYears,
                rightField = ArithmeticField.MONTHS,
                rightValue = uiState.arithmeticMonths,
                onEvent = onEvent
            )
            ArithmeticFieldRow(
                leftField = ArithmeticField.WEEKS,
                leftValue = uiState.arithmeticWeeks,
                rightField = ArithmeticField.DAYS,
                rightValue = uiState.arithmeticDays,
                onEvent = onEvent
            )
            ArithmeticFieldRow(
                leftField = ArithmeticField.HOURS,
                leftValue = uiState.arithmeticHours,
                rightField = ArithmeticField.MINUTES,
                rightValue = uiState.arithmeticMinutes,
                onEvent = onEvent
            )
        }

        if (uiState.arithmeticDateText.isNotEmpty()) {
            ResultCard(title = stringResource(R.string.date_time_calculator_arithmetic_result_title)) {
                Text(
                    text = if (uiState.arithmeticTimeText.isNotEmpty()) {
                        "${uiState.arithmeticDateText} ${uiState.arithmeticTimeText}"
                    } else {
                        uiState.arithmeticDateText
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.arithmeticWeekdayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun AgeSection(
    uiState: DateTimeCalculatorUiState,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    onPickDate: (DateTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InputCard {
            DateField(
                label = stringResource(R.string.date_time_calculator_birth_date),
                value = uiState.ageBirthDate,
                onClick = { onPickDate(DateTarget.AGE_BIRTH) }
            )
            DateField(
                label = stringResource(R.string.date_time_calculator_target_date),
                value = uiState.ageTargetDate,
                onClick = { onPickDate(DateTarget.AGE_TARGET) }
            )
            TextButton(
                onClick = {
                    onEvent(DateTimeCalculatorUiEvent.OnAgeTargetDateChange(LocalDate.now().toString()))
                }
            ) {
                Text(stringResource(R.string.date_time_calculator_today))
            }
        }

        if (uiState.ageText.isNotEmpty()) {
            ResultCard(title = stringResource(R.string.date_time_calculator_age_result_title)) {
                Text(
                    text = uiState.ageText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.ageTotalDaysText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.ageNextBirthdayText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = uiState.ageCountdownText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TimeZoneSection(
    uiState: DateTimeCalculatorUiState,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    onPickDate: (DateTarget) -> Unit,
    onPickTime: (TimeTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InputCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeZoneField(
                    label = stringResource(R.string.date_time_calculator_timezone_source),
                    zoneId = uiState.timeZoneSourceId,
                    onClick = { onEvent(DateTimeCalculatorUiEvent.OnOpenTimeZonePicker(TimeZoneTarget.SOURCE)) },
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { onEvent(DateTimeCalculatorUiEvent.OnSwapTimeZones) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = stringResource(R.string.date_time_calculator_swap_timezone)
                    )
                }
                TimeZoneField(
                    label = stringResource(R.string.date_time_calculator_timezone_target),
                    zoneId = uiState.timeZoneTargetId,
                    onClick = { onEvent(DateTimeCalculatorUiEvent.OnOpenTimeZonePicker(TimeZoneTarget.TARGET)) },
                    modifier = Modifier.weight(1f)
                )
            }
            DateField(
                label = stringResource(R.string.date_time_calculator_date),
                value = uiState.timeZoneDate,
                onClick = { onPickDate(DateTarget.TIME_ZONE) }
            )
            TimeField(
                label = stringResource(R.string.date_time_calculator_time),
                value = uiState.timeZoneTime,
                onClick = { onPickTime(TimeTarget.TIME_ZONE) }
            )
        }

        if (uiState.timeZoneResultTimeText.isNotEmpty()) {
            ResultCard(title = stringResource(R.string.date_time_calculator_timezone_result_title)) {
                Text(
                    text = "${uiState.timeZoneResultDateText} ${uiState.timeZoneResultTimeText}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.timeZoneTargetId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.timeZoneOffsetText,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = uiState.timeZoneDayChangeText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ArithmeticFieldRow(
    leftField: ArithmeticField,
    leftValue: String,
    rightField: ArithmeticField,
    rightValue: String,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ArithmeticField(
            field = leftField,
            value = leftValue,
            onEvent = onEvent,
            modifier = Modifier.weight(1f)
        )
        ArithmeticField(
            field = rightField,
            value = rightValue,
            onEvent = onEvent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ArithmeticField(
    field: ArithmeticField,
    value: String,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onEvent(DateTimeCalculatorUiEvent.OnArithmeticValueChange(field, it)) },
        modifier = modifier,
        label = { Text(arithmeticFieldLabel(field)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun arithmeticFieldLabel(field: ArithmeticField): String = stringResource(
    when (field) {
        ArithmeticField.YEARS -> R.string.date_time_calculator_unit_years
        ArithmeticField.MONTHS -> R.string.date_time_calculator_unit_months
        ArithmeticField.WEEKS -> R.string.date_time_calculator_unit_weeks
        ArithmeticField.DAYS -> R.string.date_time_calculator_unit_days
        ArithmeticField.HOURS -> R.string.date_time_calculator_unit_hours
        ArithmeticField.MINUTES -> R.string.date_time_calculator_unit_minutes
    }
)
@Composable
private fun InputCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(content = content)
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedDate(value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = stringResource(R.string.date_time_calculator_select_date)
            )
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = stringResource(R.string.date_time_calculator_select_time)
            )
        }
    }
}

@Composable
private fun TimeZoneField(
    label: String,
    zoneId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = zoneId,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorTimePickerDialog(
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.date_time_calculator_select_time)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeZonePickerSheet(
    timeZoneIds: List<String>,
    selectedZoneId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredIds = remember(timeZoneIds, query) {
        if (query.isBlank()) {
            timeZoneIds
        } else {
            timeZoneIds.filter { it.contains(query, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = 640.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.date_time_calculator_select_timezone),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.date_time_calculator_search_timezone)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            if (filteredIds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.date_time_calculator_no_timezone),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredIds, key = { it }) { zoneId ->
                        val selected = zoneId == selectedZoneId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(zoneId) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zoneId,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = zoneOffsetLabel(zoneId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private fun initialDate(target: DateTarget, state: DateTimeCalculatorUiState): LocalDate {
    val value = when (target) {
        DateTarget.INTERVAL_START -> state.intervalStartDate
        DateTarget.INTERVAL_END -> state.intervalEndDate
        DateTarget.ARITHMETIC_BASE -> state.arithmeticBaseDate
        DateTarget.AGE_BIRTH -> state.ageBirthDate
        DateTarget.AGE_TARGET -> state.ageTargetDate
        DateTarget.TIME_ZONE -> state.timeZoneDate
    }
    return DateTimeCalculatorEngine.parseDate(value) ?: LocalDate.now()
}

private fun initialTime(target: TimeTarget, state: DateTimeCalculatorUiState): LocalTime {
    val value = when (target) {
        TimeTarget.INTERVAL_START -> state.intervalStartTime
        TimeTarget.INTERVAL_END -> state.intervalEndTime
        TimeTarget.ARITHMETIC_BASE -> state.arithmeticBaseTime
        TimeTarget.TIME_ZONE -> state.timeZoneTime
    }
    return DateTimeCalculatorEngine.parseTime(value) ?: LocalTime.MIDNIGHT
}

private fun onDateSelected(
    target: DateTarget,
    date: LocalDate,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit
) {
    val event = when (target) {
        DateTarget.INTERVAL_START -> DateTimeCalculatorUiEvent.OnIntervalStartDateChange(date.toString())
        DateTarget.INTERVAL_END -> DateTimeCalculatorUiEvent.OnIntervalEndDateChange(date.toString())
        DateTarget.ARITHMETIC_BASE -> DateTimeCalculatorUiEvent.OnArithmeticBaseDateChange(date.toString())
        DateTarget.AGE_BIRTH -> DateTimeCalculatorUiEvent.OnAgeBirthDateChange(date.toString())
        DateTarget.AGE_TARGET -> DateTimeCalculatorUiEvent.OnAgeTargetDateChange(date.toString())
        DateTarget.TIME_ZONE -> DateTimeCalculatorUiEvent.OnTimeZoneDateChange(date.toString())
    }
    onEvent(event)
}

private fun onTimeSelected(
    target: TimeTarget,
    time: LocalTime,
    onEvent: (DateTimeCalculatorUiEvent) -> Unit
) {
    val text = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val event = when (target) {
        TimeTarget.INTERVAL_START -> DateTimeCalculatorUiEvent.OnIntervalStartTimeChange(text)
        TimeTarget.INTERVAL_END -> DateTimeCalculatorUiEvent.OnIntervalEndTimeChange(text)
        TimeTarget.ARITHMETIC_BASE -> DateTimeCalculatorUiEvent.OnArithmeticBaseTimeChange(text)
        TimeTarget.TIME_ZONE -> DateTimeCalculatorUiEvent.OnTimeZoneTimeChange(text)
    }
    onEvent(event)
}

@Composable
private fun formattedDate(value: String): String {
    val date = DateTimeCalculatorEngine.parseDate(value) ?: return value.ifBlank { "—" }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

@Composable
private fun zoneOffsetLabel(zoneId: String): String {
    val offset = runCatching {
        ZoneId.of(zoneId).rules.getOffset(Instant.now())
    }.getOrNull() ?: ZoneOffset.UTC
    val id = offset.id
    val text = if (id == "Z") "+00:00" else id
    return stringResource(R.string.date_time_calculator_timezone_offset_format, text)
}