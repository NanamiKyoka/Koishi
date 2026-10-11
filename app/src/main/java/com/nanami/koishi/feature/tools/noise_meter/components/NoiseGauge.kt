package com.nanami.koishi.feature.tools.noise_meter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.feature.tools.noise_meter.NoiseMeterUiState
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseLevel
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private val ZONE_BOUNDS = listOf(
    0.0 to NoiseLevel.VERY_QUIET,
    30.0 to NoiseLevel.QUIET,
    40.0 to NoiseLevel.MODERATE,
    55.0 to NoiseLevel.NOISY,
    70.0 to NoiseLevel.VERY_NOISY,
    85.0 to NoiseLevel.DANGEROUS,
    100.0 to NoiseLevel.EXTREME
)

@Composable
fun NoiseGauge(
    value: Double,
    fraction: Float,
    unit: String,
    color: Color,
    isActive: Boolean,
    levelLabel: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val valueText = if (isActive) value.toInt().toString() else "--"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val stroke = size.minDimension * 0.085f
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            ZONE_BOUNDS.forEachIndexed { index, (lowerDb, level) ->
                val upperDb = ZONE_BOUNDS.getOrNull(index + 1)?.first
                    ?: NoiseMeterUiState.MAX_DISPLAY_DB
                val zoneStart = START_ANGLE + SWEEP_ANGLE * ((lowerDb / NoiseMeterUiState.MAX_DISPLAY_DB).toFloat())
                val zoneSweep = SWEEP_ANGLE * (((upperDb - lowerDb) / NoiseMeterUiState.MAX_DISPLAY_DB).toFloat())
                drawArc(
                    color = level.color().copy(alpha = 0.35f),
                    startAngle = zoneStart,
                    sweepAngle = zoneSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
            }

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.45f, cap = StrokeCap.Round)
            )

            if (fraction > 0f) {
                drawArc(
                    color = color,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 0.6f, cap = StrokeCap.Round)
                )
            }

            val markerAngle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * fraction).toDouble())
            val centerX = topLeft.x + arcSize.width / 2f
            val centerY = topLeft.y + arcSize.height / 2f
            val markerX = centerX + (arcSize.width / 2f) * cos(markerAngle).toFloat()
            val markerY = centerY + (arcSize.height / 2f) * sin(markerAngle).toFloat()
            drawCircle(
                color = color,
                radius = stroke * 0.75f,
                center = Offset(markerX, markerY)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = levelLabel,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}