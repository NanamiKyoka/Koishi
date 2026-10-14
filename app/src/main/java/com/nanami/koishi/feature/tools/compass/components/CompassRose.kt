package com.nanami.koishi.feature.tools.compass.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.compass.CardinalDirection
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassRose(
    headingDeg: Double,
    direction: CardinalDirection,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 22.dp.toPx()
            val innerRadius = outerRadius * 0.82f
            val heading = ((headingDeg % 360.0) + 360.0) % 360.0

            drawCircle(
                color = outline,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = outline.copy(alpha = 0.6f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            repeat(72) { index ->
                val angle = Math.toRadians(index * 5.0 - 90.0 - heading)
                val cardinal = index % 18 == 0
                val major = index % 9 == 0
                val tickLength = when {
                    cardinal -> 18.dp.toPx()
                    major -> 13.dp.toPx()
                    else -> 7.dp.toPx()
                }
                val outer = Offset(
                    x = center.x + cos(angle).toFloat() * outerRadius,
                    y = center.y + sin(angle).toFloat() * outerRadius
                )
                val inner = Offset(
                    x = center.x + cos(angle).toFloat() * (outerRadius - tickLength),
                    y = center.y + sin(angle).toFloat() * (outerRadius - tickLength)
                )
                drawLine(
                    color = if (cardinal) primary else outline,
                    start = inner,
                    end = outer,
                    strokeWidth = if (cardinal) 2.5.dp.toPx() else 1.dp.toPx()
                )
            }

            val labels = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
            drawIntoCanvas { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.TRANSPARENT
                    textAlign = Paint.Align.CENTER
                    textSize = 16.dp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                labels.forEach { (label, labelAngle) ->
                    paint.color = if (label == "N") primary.toArgb() else onSurface.toArgb()
                    val angle = Math.toRadians(labelAngle - heading - 90.0)
                    val radius = outerRadius - 33.dp.toPx()
                    val x = center.x + cos(angle).toFloat() * radius
                    val baseline = center.y + sin(angle).toFloat() * radius -
                        (paint.ascent() + paint.descent()) / 2f
                    canvas.nativeCanvas.drawText(label, x, baseline, paint)
                }
            }

            val marker = Path().apply {
                moveTo(center.x, center.y - outerRadius + 7.dp.toPx())
                lineTo(center.x - 10.dp.toPx(), center.y - outerRadius + 29.dp.toPx())
                lineTo(center.x + 10.dp.toPx(), center.y - outerRadius + 29.dp.toPx())
                close()
            }
            drawPath(marker, primary)

            drawCircle(
                color = surface,
                radius = outerRadius * 0.62f,
                center = center
            )
            drawCircle(
                color = secondary.copy(alpha = 0.18f),
                radius = outerRadius * 0.62f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.compass_degrees_format, headingDeg.toInt()),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                text = stringResource(direction.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = onSurfaceVariant
            )
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
