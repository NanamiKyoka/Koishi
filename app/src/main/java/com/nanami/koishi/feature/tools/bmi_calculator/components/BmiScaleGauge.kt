package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory

@Composable
fun BmiScaleGauge(
    fraction: Float,
    category: BmiCategory,
    modifier: Modifier = Modifier
) {
    val bandColors = rememberBmiBandColors()

    Column(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val thumbSize = 20.dp
            val maxTravel = (maxWidth - thumbSize).coerceAtLeast(0.dp)
            val thumbOffset = (maxWidth * fraction - thumbSize / 2)
                .coerceIn(0.dp, maxTravel)

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BmiCategory.entries.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bandColors.of(item).copy(alpha = 0.32f))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(4.dp, bandColors.of(category), CircleShape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BmiCategory.entries.forEach { item ->
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
