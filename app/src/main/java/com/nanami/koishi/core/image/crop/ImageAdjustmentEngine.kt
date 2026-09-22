package com.nanami.koishi.core.image.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.ColorFilter as ComposeColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

object ImageAdjustmentEngine {

    /**
     * 生成恒等 4x5 矩阵
     */
    fun identityMatrix(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /**
     * 生成用于亮度、对比度、饱和度调节的 4x5 色彩矩阵数组 (FloatArray of 20 elements)
     * 参数范围：-100f .. 100f，默认 0f
     */
    fun createColorMatrixArray(brightness: Float, contrast: Float, saturation: Float): FloatArray {
        var current = identityMatrix()

        // 1. 对比度
        if (contrast != 0f) {
            val scale = if (contrast >= 0) 1f + (contrast / 100f) * 1.5f else 1f + (contrast / 100f) * 0.8f
            val offset = (1f - scale) * 128f
            val contrastMatrix = floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiplyColorMatrices(contrastMatrix, current)
        }

        // 2. 亮度
        if (brightness != 0f) {
            val bOffset = (brightness / 100f) * 255f
            val brightnessMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, bOffset,
                0f, 1f, 0f, 0f, bOffset,
                0f, 0f, 1f, 0f, bOffset,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiplyColorMatrices(brightnessMatrix, current)
        }

        // 3. 饱和度
        if (saturation != 0f) {
            val sat = if (saturation >= 0) 1f + (saturation / 100f) * 2f else (saturation + 100f) / 100f
            val invSat = 1f - sat.coerceAtLeast(0f)
            val r = 0.213f * invSat
            val g = 0.715f * invSat
            val b = 0.072f * invSat

            val satMatrix = floatArrayOf(
                r + sat, g, b, 0f, 0f,
                r, g + sat, b, 0f, 0f,
                r, g, b + sat, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            current = multiplyColorMatrices(satMatrix, current)
        }

        return current
    }

    /**
     * 两个 4x5 仿射矩阵相乘: C = A x B
     */
    fun multiplyColorMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(20)
        for (i in 0 until 4) {
            val rowOffset = i * 5
            for (j in 0 until 4) {
                result[rowOffset + j] = (
                    a[rowOffset + 0] * b[0 * 5 + j] +
                    a[rowOffset + 1] * b[1 * 5 + j] +
                    a[rowOffset + 2] * b[2 * 5 + j] +
                    a[rowOffset + 3] * b[3 * 5 + j]
                )
            }
            result[rowOffset + 4] = (
                a[rowOffset + 0] * b[0 * 5 + 4] +
                a[rowOffset + 1] * b[1 * 5 + 4] +
                a[rowOffset + 2] * b[2 * 5 + 4] +
                a[rowOffset + 3] * b[3 * 5 + 4] +
                a[rowOffset + 4]
            )
        }
        return result
    }

    /**
     * 将色彩矩阵转换为 Jetpack Compose 硬件加速 ColorFilter
     */
    fun createComposeColorFilter(brightness: Float, contrast: Float, saturation: Float): ComposeColorFilter? {
        if (brightness == 0f && contrast == 0f && saturation == 0f) return null
        val array = createColorMatrixArray(brightness, contrast, saturation)
        return ComposeColorFilter.colorMatrix(ComposeColorMatrix(array))
    }

    /**
     * 根据视口变换参数与裁剪区域，执行高保真最终位图渲染
     */
    fun renderCroppedBitmap(
        source: Bitmap,
        cropRectOnScreen: RectF,
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        rotationDegrees: Float,
        panX: Float,
        panY: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        outputMaxDimension: Int = 1024
    ): Bitmap {
        // 计算目标输出分辨率（保持裁剪框的宽高比）
        val aspect = cropRectOnScreen.width() / cropRectOnScreen.height()
        val targetWidth: Int
        val targetHeight: Int
        if (aspect >= 1f) {
            targetWidth = outputMaxDimension
            targetHeight = (outputMaxDimension / aspect).toInt().coerceAtLeast(1)
        } else {
            targetHeight = outputMaxDimension
            targetWidth = (outputMaxDimension * aspect).toInt().coerceAtLeast(1)
        }

        val outBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // 视口屏幕坐标 -> 输出画布坐标的缩放比
        val screenToCanvasScale = targetWidth.toFloat() / cropRectOnScreen.width()

        // 变换矩阵：将裁剪框左上角平移至 (0, 0)，再缩放到画布尺寸
        val matrix = Matrix()

        // 1. 将图片中心平移至视口中心 + 用户平移
        val srcCenterX = source.width / 2f
        val srcCenterY = source.height / 2f
        val viewCenterX = viewportWidth / 2f + panX
        val viewCenterY = viewportHeight / 2f + panY

        matrix.postTranslate(-srcCenterX, -srcCenterY)
        matrix.postScale(scale, scale)
        matrix.postRotate(rotationDegrees)
        matrix.postTranslate(viewCenterX, viewCenterY)

        // 2. 映射到裁剪区域：减去 cropRectOnScreen 的左上角，并乘以 screenToCanvasScale
        matrix.postTranslate(-cropRectOnScreen.left, -cropRectOnScreen.top)
        matrix.postScale(screenToCanvasScale, screenToCanvasScale)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            if (brightness != 0f || contrast != 0f || saturation != 0f) {
                val array = createColorMatrixArray(brightness, contrast, saturation)
                colorFilter = ColorMatrixColorFilter(array)
            }
        }

        canvas.drawBitmap(source, matrix, paint)
        return outBitmap
    }
}
