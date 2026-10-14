package com.nanami.koishi.feature.tools.spirit_level.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nanami.koishi.feature.tools.spirit_level.LevelStatus
import com.nanami.koishi.feature.tools.spirit_level.SpiritLevelUiState

@Composable
fun BubbleLevel(
    state: SpiritLevelUiState,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 20.dp.toPx()
        val bubbleRadius = 22.dp.toPx()
        val travel = radius - bubbleRadius - 18.dp.toPx()

        drawCircle(
            color = surface,
            radius = radius,
            center = center
        )
        listOf(0.33f, 0.66f, 1f).forEach { fraction ->
            drawCircle(
                color = outline,
                radius = radius * fraction,
                center = center,
                style = Stroke(width = if (fraction == 1f) 2.dp.toPx() else 1.dp.toPx())
            )
        }
        drawLine(
            color = outline,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = outline,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(
            color = primary.copy(alpha = 0.2f),
            radius = radius * 0.12f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val bubbleCenter = Offset(
            x = center.x + state.bubbleX * travel,
            y = center.y + state.bubbleY * travel
        )
        val bubbleColor = when (state.status) {
            LevelStatus.LEVEL -> primary
            LevelStatus.NEAR -> tertiary
            LevelStatus.OFF -> error
        }
        drawCircle(
            color = bubbleColor.copy(alpha = 0.18f),
            radius = bubbleRadius * 1.45f,
            center = bubbleCenter
        )
        drawCircle(
            color = bubbleColor,
            radius = bubbleRadius,
            center = bubbleCenter
        )
    }
}
