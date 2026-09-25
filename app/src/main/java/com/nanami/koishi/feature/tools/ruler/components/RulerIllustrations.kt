package com.nanami.koishi.feature.tools.ruler.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RulerCardIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    tint: Color = Color(0xFF8D8375)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        rotate(degrees = -40f, pivot = Offset(w / 2, h / 2)) {
            val rulerWidth = w * 0.95f
            val rulerHeight = h * 0.38f
            val left = (w - rulerWidth) / 2
            val top = (h - rulerHeight) / 2

            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + rulerWidth,
                        bottom = top + rulerHeight,
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                )
            }
            drawPath(path = path, color = tint, style = Stroke(width = 8f))

            val tickCount = 7
            val tickSpacing = rulerWidth / (tickCount + 1)
            for (i in 1..tickCount) {
                val tickX = left + i * tickSpacing
                val tickHeight = if (i % 2 == 0) rulerHeight * 0.45f else rulerHeight * 0.28f
                drawLine(
                    color = tint,
                    start = Offset(tickX, top),
                    end = Offset(tickX, top + tickHeight),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ProtractorCardIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    tint: Color = Color(0xFF3C6E71)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.85f
        val cy = h * 0.85f
        val radius = w * 0.7f

        val path = Path().apply {
            moveTo(cx - radius, cy)
            lineTo(cx, cy)
            lineTo(cx - radius * cos(Math.toRadians(70.0)).toFloat(), cy - radius * sin(Math.toRadians(70.0)).toFloat())
        }
        drawPath(path = path, color = tint, style = Stroke(width = 8f))

        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 8f)
        )

        val ticks = listOf(195f, 210f, 225f, 240f)
        for (angle in ticks) {
            val rad = Math.toRadians(angle.toDouble())
            val innerR = radius * 0.72f
            val x1 = cx + innerR * cos(rad).toFloat()
            val y1 = cy + innerR * sin(rad).toFloat()
            val x2 = cx + radius * cos(rad).toFloat()
            val y2 = cy + radius * sin(rad).toFloat()
            drawLine(
                color = tint,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}
