package com.nanami.koishi.feature.tools.color_picker.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ColorLoupe(
    bitmap: Bitmap,
    cursorX: Int,
    cursorY: Int,
    selectedColor: Color,
    magnification: Float,
    displayedWidth: Float,
    displayedHeight: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
    radius: Dp = 68.dp
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@Canvas

        val radiusPx = radius.toPx()
        val screenTargetX = offsetX + (cursorX + 0.5f) / bitmap.width.toFloat() * displayedWidth
        val screenTargetY = offsetY + (cursorY + 0.5f) / bitmap.height.toFloat() * displayedHeight

        val baseDisplayScale = displayedWidth / bitmap.width.toFloat()
        val scaleLoupe = baseDisplayScale * magnification

        val clipPath = Path().apply {
            addOval(Rect(center = Offset(screenTargetX, screenTargetY), radius = radiusPx))
        }

        clipPath(clipPath) {
            drawRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(screenTargetX - radiusPx, screenTargetY - radiusPx),
                size = Size(radiusPx * 2f, radiusPx * 2f)
            )

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(screenTargetX, screenTargetY)
            drawContext.canvas.nativeCanvas.scale(scaleLoupe, scaleLoupe)
            drawContext.canvas.nativeCanvas.translate(-(cursorX + 0.5f), -(cursorY + 0.5f))

            val bitmapPaint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            drawContext.canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
            drawContext.canvas.nativeCanvas.restore()

            val isLightColor = selectedColor.luminance() > 0.5f
            val crosshairColor = if (isLightColor) Color(0xFF212121) else Color(0xFFFFFFFF)
            val crosshairStroke = 1.5.dp.toPx()

            drawLine(
                color = crosshairColor,
                start = Offset(screenTargetX - radiusPx, screenTargetY),
                end = Offset(screenTargetX + radiusPx, screenTargetY),
                strokeWidth = crosshairStroke
            )
            drawLine(
                color = crosshairColor,
                start = Offset(screenTargetX, screenTargetY - radiusPx),
                end = Offset(screenTargetX, screenTargetY + radiusPx),
                strokeWidth = crosshairStroke
            )

            val targetBoxHalf = (scaleLoupe / 2f).coerceIn(2.5.dp.toPx(), 7.dp.toPx())
            drawRect(
                color = crosshairColor,
                topLeft = Offset(screenTargetX - targetBoxHalf, screenTargetY - targetBoxHalf),
                size = Size(targetBoxHalf * 2f, targetBoxHalf * 2f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        val ringColor = if (selectedColor.luminance() > 0.5f) {
            Color(0xFF383838)
        } else {
            Color(0xFFF2F2F2)
        }
        val ringStrokeWidth = 7.dp.toPx()

        drawCircle(
            color = ringColor,
            radius = radiusPx,
            center = Offset(screenTargetX, screenTargetY),
            style = Stroke(width = ringStrokeWidth)
        )

        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = radiusPx + ringStrokeWidth / 2f,
            center = Offset(screenTargetX, screenTargetY),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
