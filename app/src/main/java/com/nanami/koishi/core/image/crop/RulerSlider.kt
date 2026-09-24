package com.nanami.koishi.core.image.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 经典相机级标尺刻度滑动器 (Ruler Dial Slider)
 */
@Composable
fun RulerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val tickSpacingDp = 8.dp
    val density = LocalDensity.current
    val spacingPx = with(density) { tickSpacingDp.toPx() }
    val dragSlopPx = with(density) { 3.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(valueRange, step, spacingPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDragging = false
                    var lastX = down.position.x
                    var totalDeltaX = 0f
                    var gestureValue = currentValue

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.find { it.id == down.id } ?: break

                        if (!pointer.pressed) {
                            // 手指抬起，若未发生拖拽则响应点击
                            if (!isDragging) {
                                val centerX = size.width / 2f
                                val deltaX = pointer.position.x - centerX
                                val deltaValue = (deltaX / spacingPx) * step
                                val newValue = (currentValue + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                                currentOnValueChange(newValue)
                            }
                            break
                        }

                        val currentX = pointer.position.x
                        val deltaX = currentX - lastX
                        totalDeltaX += deltaX
                        lastX = currentX

                        if (!isDragging && abs(totalDeltaX) >= dragSlopPx) {
                            isDragging = true
                            gestureValue = currentValue
                            pointer.consume()
                            val deltaValue = -(totalDeltaX / spacingPx) * step
                            gestureValue = (gestureValue + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                            currentOnValueChange(gestureValue)
                        } else if (isDragging) {
                            pointer.consume()
                            val deltaValue = -(deltaX / spacingPx) * step
                            gestureValue = (gestureValue + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                            currentOnValueChange(gestureValue)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val fadeWidth = 44.dp.toPx()

            // 当前中心值对应的刻度位置索引
            val centerIndex = currentValue / step
            val visibleCount = (w / (2f * spacingPx)).toInt() + 4
            val startIndex = (centerIndex - visibleCount).toInt()
            val endIndex = (centerIndex + visibleCount).toInt()

            // 绘制所有可见刻度线
            for (i in startIndex..endIndex) {
                val x = centerX + (i - centerIndex) * spacingPx
                if (x in 0f..w) {
                    val isMajor = i % 5 == 0
                    val isTen = i % 10 == 0

                    val tickHeight = when {
                        isTen -> h * 0.55f
                        isMajor -> h * 0.42f
                        else -> h * 0.28f
                    }

                    // 左右边缘淡出渐隐 (Fade Out)
                    val distToEdge = min(x, w - x)
                    val edgeAlpha = (distToEdge / fadeWidth).coerceIn(0f, 1f)

                    val baseAlpha = if (isMajor) 0.8f else 0.35f
                    val tickColor = (if (isMajor) onSurfaceColor else onSurfaceVariantColor)
                        .copy(alpha = baseAlpha * edgeAlpha)

                    val startY = (h - tickHeight) / 2f
                    val endY = startY + tickHeight

                    drawLine(
                        color = tickColor,
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 绘制中央指示标针 (高亮主题色，圆角胶囊状)
            val indicatorHeight = h * 0.65f
            val indStartY = (h - indicatorHeight) / 2f
            val indEndY = indStartY + indicatorHeight

            drawLine(
                color = primaryColor,
                start = Offset(centerX, indStartY),
                end = Offset(centerX, indEndY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
