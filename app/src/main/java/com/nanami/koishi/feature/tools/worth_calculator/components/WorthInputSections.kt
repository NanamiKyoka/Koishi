package com.nanami.koishi.feature.tools.worth_calculator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.worth_calculator.WorthCalculatorUiEvent
import com.nanami.koishi.feature.tools.worth_calculator.WorthCalculatorUiState
import com.nanami.koishi.feature.tools.worth_calculator.engine.CanteenQuality
import com.nanami.koishi.feature.tools.worth_calculator.engine.CityTier
import com.nanami.koishi.feature.tools.worth_calculator.engine.DegreeType
import com.nanami.koishi.feature.tools.worth_calculator.engine.JobStability
import com.nanami.koishi.feature.tools.worth_calculator.engine.LeadershipRelation
import com.nanami.koishi.feature.tools.worth_calculator.engine.SchoolTier
import com.nanami.koishi.feature.tools.worth_calculator.engine.ShuttleQuality
import com.nanami.koishi.feature.tools.worth_calculator.engine.TeamAtmosphere
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorkEnvironment
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorkYears
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCountries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryAndRegionSection(
    state: WorthCalculatorUiState,
    onEvent: (WorthCalculatorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(title = stringResource(R.string.worth_section_salary_region))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.salaryInput,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnSalaryChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_salary)) },
                    prefix = { Text(state.selectedCountry.currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = stringResource(state.selectedCountry.nameRes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.worth_input_country)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        WorthCountries.supportedCountries.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Text("${stringResource(country.nameRes)} (${country.currencySymbol} · PPP ${country.pppFactor})")
                                },
                                onClick = {
                                    onEvent(WorthCalculatorUiEvent.OnCountryChange(country))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkScheduleSection(
    state: WorthCalculatorUiState,
    onEvent: (WorthCalculatorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(title = stringResource(R.string.worth_section_schedule))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.workDaysPerWeek,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnWorkDaysChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_work_days)) },
                    suffix = { Text("天/周") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.wfhDaysPerWeek,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnWfhDaysChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_wfh_days)) },
                    suffix = { Text("天/周") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.annualLeave,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnAnnualLeaveChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_annual_leave)) },
                    suffix = { Text("天") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.publicHolidays,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnPublicHolidaysChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_public_holidays)) },
                    suffix = { Text("天") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.paidSickLeave,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnPaidSickLeaveChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_paid_sick_leave)) },
                    suffix = { Text("天") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.workHours,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnWorkHoursChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_daily_work_hours)) },
                    suffix = { Text("h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.commuteHours,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnCommuteHoursChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_daily_commute)) },
                    suffix = { Text("h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.restTime,
                    onValueChange = { onEvent(WorthCalculatorUiEvent.OnRestTimeChange(it)) },
                    label = { Text(stringResource(R.string.worth_input_daily_rest)) },
                    suffix = { Text("h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvironmentAndTeamSection(
    state: WorthCalculatorUiState,
    onEvent: (WorthCalculatorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(title = stringResource(R.string.worth_section_environment_team))

            SubItemTitle(title = stringResource(R.string.worth_label_job_stability))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                JobStability.entries.forEach { item ->
                    FilterChip(
                        selected = state.jobStability == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnJobStabilityChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            SubItemTitle(title = stringResource(R.string.worth_label_city_tier))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CityTier.entries.forEach { item ->
                    FilterChip(
                        selected = state.cityTier == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnCityTierChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            SubItemTitle(title = stringResource(R.string.worth_label_work_environment))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WorkEnvironment.entries.forEach { item ->
                    FilterChip(
                        selected = state.workEnvironment == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnWorkEnvironmentChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            SubItemTitle(title = stringResource(R.string.worth_label_leadership))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LeadershipRelation.entries.forEach { item ->
                    FilterChip(
                        selected = state.leadership == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnLeadershipChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            SubItemTitle(title = stringResource(R.string.worth_label_teamwork))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TeamAtmosphere.entries.forEach { item ->
                    FilterChip(
                        selected = state.teamwork == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnTeamworkChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(WorthCalculatorUiEvent.OnIsHometownChange(!state.isHometown)) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.worth_label_hometown),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = state.isHometown,
                    onCheckedChange = { onEvent(WorthCalculatorUiEvent.OnIsHometownChange(it)) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(WorthCalculatorUiEvent.OnHasShuttleChange(!state.hasShuttle)) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.worth_label_shuttle),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = state.hasShuttle,
                    onCheckedChange = { onEvent(WorthCalculatorUiEvent.OnHasShuttleChange(it)) }
                )
            }

            AnimatedVisibility(visible = state.hasShuttle) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShuttleQuality.entries.forEach { item ->
                        FilterChip(
                            selected = state.shuttleQuality == item,
                            onClick = { onEvent(WorthCalculatorUiEvent.OnShuttleQualityChange(item)) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(WorthCalculatorUiEvent.OnHasCanteenChange(!state.hasCanteen)) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.worth_label_canteen),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = state.hasCanteen,
                    onCheckedChange = { onEvent(WorthCalculatorUiEvent.OnHasCanteenChange(it)) }
                )
            }

            AnimatedVisibility(visible = state.hasCanteen) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CanteenQuality.entries.forEach { item ->
                        FilterChip(
                            selected = state.canteenQuality == item,
                            onClick = { onEvent(WorthCalculatorUiEvent.OnCanteenQualityChange(item)) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EducationAndExperienceSection(
    state: WorthCalculatorUiState,
    onEvent: (WorthCalculatorUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle(title = stringResource(R.string.worth_section_education_experience))

            SubItemTitle(title = stringResource(R.string.worth_label_degree_type))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DegreeType.entries.forEach { item ->
                    FilterChip(
                        selected = state.degreeType == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnDegreeTypeChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }

            if (state.degreeType != DegreeType.BELOW_BACHELOR) {
                SubItemTitle(
                    title = if (state.degreeType == DegreeType.BACHELOR) {
                        stringResource(R.string.worth_label_school_tier_bachelor)
                    } else {
                        stringResource(R.string.worth_label_school_tier_postgrad)
                    }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SchoolTier.entries.forEach { item ->
                        val labelRes = if (state.degreeType == DegreeType.BACHELOR) {
                            item.bachelorLabelRes
                        } else {
                            item.postgradLabelRes
                        }
                        FilterChip(
                            selected = state.schoolTier == item,
                            onClick = { onEvent(WorthCalculatorUiEvent.OnSchoolTierChange(item)) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }

            if (state.degreeType == DegreeType.MASTERS) {
                SubItemTitle(title = stringResource(R.string.worth_label_bachelor_tier))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SchoolTier.entries.forEach { item ->
                        FilterChip(
                            selected = state.bachelorTier == item,
                            onClick = { onEvent(WorthCalculatorUiEvent.OnBachelorTierChange(item)) },
                            label = { Text(stringResource(item.bachelorLabelRes)) }
                        )
                    }
                }
            }

            SubItemTitle(title = stringResource(R.string.worth_label_work_years))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WorkYears.entries.forEach { item ->
                    FilterChip(
                        selected = state.workYears == item,
                        onClick = { onEvent(WorthCalculatorUiEvent.OnWorkYearsChange(item)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SubItemTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
