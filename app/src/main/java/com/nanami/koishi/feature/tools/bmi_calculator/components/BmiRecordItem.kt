package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.bmi_calculator.BmiRecordUi
import com.nanami.koishi.feature.tools.bmi_calculator.BmiTrendDirection

@Composable
fun BmiRecordItem(
    record: BmiRecordUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bandColors = rememberBmiBandColors()
    val accentColor = bandColors.of(record.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) {
            BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.75f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.isLatest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.bmi_record_latest),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.bmi_record_bmi, record.bmiText),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BmiCategoryChip(category = record.category)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.bmi_record_detail,
                            record.heightText,
                            record.weightText
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (record.deltaDirection != BmiTrendDirection.FLAT) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = record.deltaDirection.trendIcon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = stringResource(
                                if (record.deltaDirection == BmiTrendDirection.UP) {
                                    R.string.bmi_record_delta_up
                                } else {
                                    R.string.bmi_record_delta_down
                                },
                                record.deltaText
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (record.deltaText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.bmi_record_delta_same),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.bmi_delete_record),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

private fun BmiTrendDirection.trendIcon(): ImageVector = when (this) {
    BmiTrendDirection.UP -> Icons.AutoMirrored.Rounded.TrendingUp
    BmiTrendDirection.DOWN -> Icons.AutoMirrored.Rounded.TrendingDown
    BmiTrendDirection.FLAT -> Icons.AutoMirrored.Rounded.TrendingFlat
}
