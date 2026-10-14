package com.nanami.koishi.feature.tools.spirit_level.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.spirit_level.SpiritLevelUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InclineGauge(
    state: SpiritLevelUiState,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(1.65f),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.65f)) {
            val center = Offset(size.width / 2f, size.height * 0.82f)
            val radius = size.width * 0.38f
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawArc(
                color = outline,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx())
            )

            repeat(37) { index ->
                val angleDeg = index * 5f
                val major = index % 9 == 0
                val middle = index % 3 == 0
                val angleRad = Math.toRadians((180.0 + angleDeg))
                val outer = Offset(
                    x = center.x + cos(angleRad).toFloat() * radius,
                    y = center.y + sin(angleRad).toFloat() * radius
                )
                val tickLength = when {
                    major -> 16.dp.toPx()
                    middle -> 11.dp.toPx()
                    else -> 7.dp.toPx()
                }
                val inner = Offset(
                    x = center.x + cos(angleRad).toFloat() * (radius - tickLength),
                    y = center.y + sin(angleRad).toFloat() * (radius - tickLength)
                )
                drawLine(
                    color = if (major) primary else outline,
                    start = inner,
                    end = outer,
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            val pointerRad = Math.toRadians(180.0 + state.frame.tiltDeg.coerceIn(0.0, 180.0))
            val pointerEnd = Offset(
                x = center.x + cos(pointerRad).toFloat() * (radius - 18.dp.toPx()),
                y = center.y + sin(pointerRad).toFloat() * (radius - 18.dp.toPx())
            )
            drawLine(
                color = primary,
                start = center,
                end = pointerEnd,
                strokeWidth = 4.dp.toPx()
            )
            drawCircle(
                color = primary,
                radius = 9.dp.toPx(),
                center = pointerEnd
            )
            drawCircle(
                color = primary,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        Column(
            modifier = Modifier.padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.spirit_level_angle_format, state.frame.tiltDeg),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                text = stringResource(R.string.spirit_level_tilt_angle),
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant
            )
        }
    }
}
