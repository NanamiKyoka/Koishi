package com.nanami.koishi.feature.tools.worth_calculator.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.worth_calculator.WorthCalculatorUiState
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthAssessmentLevel
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorthResultView(
    state: WorthCalculatorUiState,
    onSaveClick: () -> Unit,
    onApplyRecord: (WorthRecord) -> Unit,
    onBackToForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val record = state.activeDetailRecord
    val result = state.result

    val score = record?.score ?: (result?.score ?: 0.0)
    val assessment = if (record != null) {
        WorthAssessmentLevel.entries.find { it.id == record.assessmentId } ?: WorthAssessmentLevel.AVERAGE
    } else {
        result?.assessment ?: WorthAssessmentLevel.AVERAGE
    }

    val currency = if (record != null) {
        record.salaryText.firstOrNull { !it.isDigit() && it != '.' }?.toString() ?: "¥"
    } else {
        state.selectedCountry.currencySymbol
    }

    val dailySalaryText = record?.dailySalaryText ?: "$currency${String.format(Locale.US, "%.1f", result?.dailySalary ?: 0.0)}"
    val workingDaysText = record?.workingDaysText ?: "${String.format(Locale.US, "%.0f", result?.workingDaysPerYear ?: 0.0)} 天"
    val effectiveHoursText = record?.effectiveHoursText?.ifBlank { null }
        ?: "${String.format(Locale.US, "%.1f", result?.effectiveDailyHours ?: 0.0)} h"

    val envFactor = record?.environmentFactor ?: (result?.environmentFactor ?: 1.0)
    val eduFactor = record?.educationFactor ?: (result?.educationFactor ?: 1.0)
    val expMultiplier = record?.experienceMultiplier ?: (result?.experienceMultiplier ?: 1.0)
    val adviceList = record?.adviceList ?: (result?.adviceList ?: emptyList())
    val salaryDisplay = record?.salaryText ?: "$currency${state.salaryInput}"

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val recordTimeText = remember(record?.timestamp) {
        record?.timestamp?.let { dateFormat.format(Date(it)) }
    }

    val badgeColor by animateColorAsState(
        targetValue = when (assessment) {
            WorthAssessmentLevel.TERRIBLE -> MaterialTheme.colorScheme.error
            WorthAssessmentLevel.POOR -> MaterialTheme.colorScheme.errorContainer
            WorthAssessmentLevel.AVERAGE -> MaterialTheme.colorScheme.tertiary
            WorthAssessmentLevel.GOOD -> MaterialTheme.colorScheme.primary
            WorthAssessmentLevel.GREAT -> MaterialTheme.colorScheme.primaryContainer
            WorthAssessmentLevel.EXCELLENT, WorthAssessmentLevel.PERFECT -> MaterialTheme.colorScheme.secondary
        },
        label = "badgeColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (record != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        recordTimeText?.let {
                            Text(
                                text = stringResource(R.string.worth_record_time_format, it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.worth_score_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stringResource(R.string.worth_input_salary)}: $salaryDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        contentColor = badgeColor
                    ) {
                        Text(
                            text = stringResource(assessment.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f", score),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(assessment.summaryRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResultMetricChip(
                        icon = Icons.Rounded.Payments,
                        label = stringResource(R.string.worth_metric_daily_salary),
                        value = dailySalaryText,
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricChip(
                        icon = Icons.Rounded.CalendarMonth,
                        label = stringResource(R.string.worth_metric_annual_days),
                        value = workingDaysText,
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricChip(
                        icon = Icons.Rounded.Schedule,
                        label = stringResource(R.string.worth_metric_effective_hours),
                        value = effectiveHoursText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Functions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.worth_factors_breakdown),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FactorItem(
                        label = stringResource(R.string.worth_factor_env),
                        value = "${String.format(Locale.US, "%.2f", envFactor)}x",
                        modifier = Modifier.weight(1f)
                    )
                    FactorItem(
                        label = stringResource(R.string.worth_factor_edu),
                        value = "${String.format(Locale.US, "%.2f", eduFactor)}x",
                        modifier = Modifier.weight(1f)
                    )
                    FactorItem(
                        label = stringResource(R.string.worth_factor_exp),
                        value = "${String.format(Locale.US, "%.2f", expMultiplier)}x",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (adviceList.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ToolCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.worth_analysis_advice_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    adviceList.forEach { adviceRes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = stringResource(adviceRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (record == null) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.worth_save_record),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onBackToForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.worth_recalculate),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = { onApplyRecord(record) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.worth_apply_to_form),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onBackToForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.worth_back_to_form),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultMetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FactorItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
