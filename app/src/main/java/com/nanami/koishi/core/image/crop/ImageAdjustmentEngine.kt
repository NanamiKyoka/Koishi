package com.nanami.koishi.core.image.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter as ComposeColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

        // 对比度
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

        // 亮度
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

        // 饱和度
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
     * 计算底图适配裁剪框的基础缩放比 (center-crop 铺满裁剪框，避免四周留黑边或透明)
     */
    fun calculateBaseScale(
        sourceWidth: Float,
        sourceHeight: Float,
        cropBoxWidth: Float,
        cropBoxHeight: Float
    ): Float {
        return if (sourceWidth > 0f && sourceHeight > 0f && cropBoxWidth > 0f && cropBoxHeight > 0f) {
            maxOf(cropBoxWidth / sourceWidth, cropBoxHeight / sourceHeight)
        } else {
            1f
        }
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
        outputMaxDimension: Int = 1024,
        baseScaleOverride: Float? = null
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

        // 计算底图适配裁剪框的基础缩放比 (优先使用传入的视口实际基础缩放比)
        val baseScale = baseScaleOverride ?: calculateBaseScale(
            sourceWidth = source.width.toFloat(),
            sourceHeight = source.height.toFloat(),
            cropBoxWidth = cropRectOnScreen.width(),
            cropBoxHeight = cropRectOnScreen.height()
        )
        val effectiveScale = scale * baseScale

        // 变换矩阵：将裁剪框左上角平移至 (0, 0)，再缩放到画布尺寸
        val matrix = Matrix()

        // 将图片中心平移至视口中心 + 用户平移
        val srcCenterX = source.width / 2f
        val srcCenterY = source.height / 2f
        val viewCenterX = viewportWidth / 2f + panX
        val viewCenterY = viewportHeight / 2f + panY

        matrix.postTranslate(-srcCenterX, -srcCenterY)
        matrix.postScale(effectiveScale, effectiveScale)
        matrix.postRotate(rotationDegrees)
        matrix.postTranslate(viewCenterX, viewCenterY)

        // 映射到裁剪区域：减去 cropRectOnScreen 的左上角，并乘以 screenToCanvasScale
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

    /**
     * 计算限制在图片范围内的有效缩放与平移偏移量
     *
     * @param pan 当前屏幕坐标系中的平移偏移量
     * @param scale 当前缩放倍数
     * @param rotationDegrees 当前旋转角度 (度数)
     * @param sourceWidth 原图宽
     * @param sourceHeight 原图高
     * @param cropBoxWidth 裁剪框宽
     * @param cropBoxHeight 裁剪框高
     * @param constrainToImage 是否开启限定在图片范围内
     * @return 经几何截断后的合法 (Offset, Scale)
     */
    fun clampPanAndScale(
        pan: Offset,
        scale: Float,
        rotationDegrees: Float,
        sourceWidth: Float,
        sourceHeight: Float,
        cropBoxWidth: Float,
        cropBoxHeight: Float,
        constrainToImage: Boolean
    ): Pair<Offset, Float> {
        if (!constrainToImage) {
            // 未开启限定：允许自由缩小至 0.5f，允许自由平移
            return Pair(pan, scale.coerceIn(0.5f, 4.0f))
        }

        val baseScale = calculateBaseScale(sourceWidth, sourceHeight, cropBoxWidth, cropBoxHeight)
        val rad = Math.toRadians(rotationDegrees.toDouble())
        val cosA = abs(cos(rad)).toFloat()
        val sinA = abs(sin(rad)).toFloat()

        // 裁剪框在旋转后图片局部坐标系下的半宽与半高投影
        val halfCropW = cropBoxWidth / 2f
        val halfCropH = cropBoxHeight / 2f
        val projHalfW = halfCropW * cosA + halfCropH * sinA
        val projHalfH = halfCropW * sinA + halfCropH * cosA

        // 图片经 baseScale 适配后的基础渲染宽高
        val drawW = sourceWidth * baseScale
        val drawH = sourceHeight * baseScale

        // 为保证旋转后的图片能完全包裹裁剪框所需的最小缩放比
        val minScaleW = if (drawW > 0f) (projHalfW * 2f) / drawW else 1f
        val minScaleH = if (drawH > 0f) (projHalfH * 2f) / drawH else 1f
        val minRequiredScale = maxOf(minScaleW, minScaleH, 1.0f)

        val clampedScale = scale.coerceIn(minRequiredScale, 4.0f)

        // 在 clampedScale 下，图片局部坐标系中允许的平移量最大值
        val currentLocalHalfW = (drawW * clampedScale) / 2f
        val currentLocalHalfH = (drawH * clampedScale) / 2f

        val maxLocalPanX = maxOf(0f, currentLocalHalfW - projHalfW)
        val maxLocalPanY = maxOf(0f, currentLocalHalfH - projHalfH)

        // 将屏幕平移向量旋转到图片局部坐标系 (-rotationDegrees)
        val negRad = -rad
        val localPanX = (pan.x * cos(negRad) - pan.y * sin(negRad)).toFloat()
        val localPanY = (pan.x * sin(negRad) + pan.y * cos(negRad)).toFloat()

        // 在图片自身坐标系中截断限制平移
        val clampedLocalPanX = localPanX.coerceIn(-maxLocalPanX, maxLocalPanX)
        val clampedLocalPanY = localPanY.coerceIn(-maxLocalPanY, maxLocalPanY)

        // 旋转回屏幕坐标系
        val screenPanX = (clampedLocalPanX * cos(rad) - clampedLocalPanY * sin(rad)).toFloat()
        val screenPanY = (clampedLocalPanX * sin(rad) + clampedLocalPanY * cos(rad)).toFloat()

        return Pair(Offset(screenPanX, screenPanY), clampedScale)
    }
}
