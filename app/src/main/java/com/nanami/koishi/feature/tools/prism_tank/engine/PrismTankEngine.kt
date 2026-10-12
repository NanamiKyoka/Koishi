package com.nanami.koishi.feature.tools.prism_tank.engine

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 光棱坦克核心算法。
 *
 * 合成阶段按斜纹图案逐像素决定归属：里图被压缩进 [0, innerThreshold] 暗部区间，
 * 表图被压缩进 [coverThreshold, 255] 亮度区间，两段区间不重叠，因此正常观看时
 * 只能看到表图；把图片亮度与曝光拉高后表图像素过曝为白，里图从暗部显影。
 *
 * 还原阶段把落在里图区间内的像素线性拉伸回 0..255，区间外的表图占位像素按
 * 填充方式补洞；表图还原则反过来，把里图占位像素补掉。
 */
object PrismTankEngine {

    private const val MAX_INPAINT_PASSES = 8

    fun isCoverPixel(x: Int, y: Int, params: PrismTankParams): Boolean {
        val gap = params.gap.coerceIn(PrismTankParams.MIN_GAP, PrismTankParams.MAX_GAP)
        val slope = params.slope.coerceIn(PrismTankParams.MIN_SLOPE, PrismTankParams.MAX_SLOPE)
        val position = if (params.isRow) y / slope + x else x / slope + y
        return position % (gap + 1) < gap
    }

    fun encodePixels(
        innerPixels: IntArray,
        coverPixels: IntArray,
        width: Int,
        height: Int,
        params: PrismTankParams = PrismTankParams()
    ): IntArray {
        val innerThreshold = params.usableInnerThreshold
        val coverThreshold = params.usableCoverThreshold
        val output = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val fromCover = isCoverPixel(x, y, params)
                val pixel = if (fromCover) coverPixels[index] else innerPixels[index]
                val luma = luma(pixel)
                val contrast = if (fromCover) params.coverContrast else params.innerContrast
                val gray = if (fromCover) params.isCoverGray else params.isInnerGray
                val threshold = if (fromCover) coverThreshold else innerThreshold

                output[index] = toArgb(
                    255,
                    scaleChannel(channel(getRed(pixel), luma, gray), contrast, threshold, fromCover, params.isReverse),
                    scaleChannel(channel(getGreen(pixel), luma, gray), contrast, threshold, fromCover, params.isReverse),
                    scaleChannel(channel(getBlue(pixel), luma, gray), contrast, threshold, fromCover, params.isReverse)
                )
            }
        }
        return output
    }

    fun createPrismTank(
        innerBitmap: Bitmap,
        coverBitmap: Bitmap,
        params: PrismTankParams = PrismTankParams()
    ): Bitmap {
        val targetWidth = max(innerBitmap.width, coverBitmap.width)
        val targetHeight = max(innerBitmap.height, coverBitmap.height)

        val scaledInner = if (innerBitmap.width == targetWidth && innerBitmap.height == targetHeight) {
            innerBitmap
        } else {
            Bitmap.createScaledBitmap(innerBitmap, targetWidth, targetHeight, true)
        }
        val scaledCover = if (coverBitmap.width == targetWidth && coverBitmap.height == targetHeight) {
            coverBitmap
        } else {
            Bitmap.createScaledBitmap(coverBitmap, targetWidth, targetHeight, true)
        }

        val innerPixels = IntArray(targetWidth * targetHeight)
        val coverPixels = IntArray(targetWidth * targetHeight)
        scaledInner.getPixels(innerPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        scaledCover.getPixels(coverPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        if (scaledInner != innerBitmap) scaledInner.recycle()
        if (scaledCover != coverBitmap) scaledCover.recycle()

        val outputPixels = encodePixels(innerPixels, coverPixels, targetWidth, targetHeight, params)
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        return result
    }

    fun revealInnerPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        config: PrismTankRevealConfig = PrismTankRevealConfig()
    ): IntArray {
        val lower = config.usableLower
        val upper = config.usableUpper
        val output = IntArray(width * height)
        val resolved = BooleanArray(width * height)
        val hole = config.fill.flatColor()

        for (index in pixels.indices) {
            val pixel = pixels[index]
            if (luma(pixel).roundToInt() in lower..upper) {
                output[index] = stretch(pixel, lower, upper - lower, config.contrast)
                resolved[index] = true
            } else {
                output[index] = hole
            }
        }

        if (config.fill == PrismTankFill.NEIGHBOR_AVG) {
            inpaint(output, resolved, width, height)
        }
        return output
    }

    fun revealCoverPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        config: PrismTankRevealConfig = PrismTankRevealConfig()
    ): IntArray {
        val lower = config.usableLower
        val upper = config.usableUpper
        val output = IntArray(pixels.size)
        val resolved = BooleanArray(pixels.size)

        for (index in pixels.indices) {
            val pixel = pixels[index]
            if (luma(pixel).roundToInt() in lower..upper) {
                output[index] = toArgb(255, 0, 0, 0)
            } else {
                output[index] = pixel
                resolved[index] = true
            }
        }

        inpaint(output, resolved, width, height)
        return output
    }

    fun applyExposurePixels(pixels: IntArray, gain: Float): IntArray {
        val safeGain = gain.coerceAtLeast(0f)
        return IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            toArgb(
                getAlpha(pixel),
                (getRed(pixel) * safeGain).roundToInt().coerceIn(0, 255),
                (getGreen(pixel) * safeGain).roundToInt().coerceIn(0, 255),
                (getBlue(pixel) * safeGain).roundToInt().coerceIn(0, 255)
            )
        }
    }

    private fun inpaint(output: IntArray, resolved: BooleanArray, width: Int, height: Int) {
        var pass = 0
        while (pass < MAX_INPAINT_PASSES) {
            var changed = false
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    if (resolved[index]) continue

                    var count = 0
                    var alpha = 0
                    var red = 0
                    var green = 0
                    var blue = 0

                    if (x > 0 && resolved[index - 1]) {
                        val pixel = output[index - 1]
                        alpha += getAlpha(pixel); red += getRed(pixel); green += getGreen(pixel); blue += getBlue(pixel); count++
                    }
                    if (x + 1 < width && resolved[index + 1]) {
                        val pixel = output[index + 1]
                        alpha += getAlpha(pixel); red += getRed(pixel); green += getGreen(pixel); blue += getBlue(pixel); count++
                    }
                    if (y > 0 && resolved[index - width]) {
                        val pixel = output[index - width]
                        alpha += getAlpha(pixel); red += getRed(pixel); green += getGreen(pixel); blue += getBlue(pixel); count++
                    }
                    if (y + 1 < height && resolved[index + width]) {
                        val pixel = output[index + width]
                        alpha += getAlpha(pixel); red += getRed(pixel); green += getGreen(pixel); blue += getBlue(pixel); count++
                    }

                    if (count == 0) continue
                    output[index] = toArgb(alpha / count, red / count, green / count, blue / count)
                    resolved[index] = true
                    changed = true
                }
            }
            if (!changed) return
            pass++
        }
    }

    private fun channel(value: Int, luma: Float, gray: Boolean): Float =
        if (gray) luma else value.toFloat()

    private fun scaleChannel(
        value: Float,
        contrast: Float,
        threshold: Int,
        fromCover: Boolean,
        reverse: Boolean
    ): Int {
        val adjusted = adjustContrast(value, contrast)
        val scaled = when {
            reverse && fromCover -> adjusted * (255 - threshold) / 255f
            reverse -> 255 - threshold + adjusted * threshold / 255f
            fromCover -> threshold + adjusted * (255 - threshold) / 255f
            else -> adjusted * threshold / 255f
        }
        return scaled.roundToInt().coerceIn(0, 255)
    }

    private fun stretch(pixel: Int, lower: Int, span: Int, contrast: Float): Int {
        val safeSpan = span.coerceAtLeast(1)
        return toArgb(
            255,
            stretchChannel(getRed(pixel), lower, safeSpan, contrast),
            stretchChannel(getGreen(pixel), lower, safeSpan, contrast),
            stretchChannel(getBlue(pixel), lower, safeSpan, contrast)
        )
    }

    private fun stretchChannel(value: Int, lower: Int, span: Int, contrast: Float): Int =
        adjustContrast((value - lower) * 255f / span, contrast).roundToInt().coerceIn(0, 255)

    private fun adjustContrast(value: Float, contrast: Float): Float {
        val factor = contrast.coerceIn(PrismTankParams.MIN_CONTRAST, PrismTankParams.MAX_CONTRAST)
        return ((value - 127.5f) * factor + 127.5f).coerceIn(0f, 255f)
    }

    private fun luma(pixel: Int): Float =
        0.299f * getRed(pixel) + 0.587f * getGreen(pixel) + 0.114f * getBlue(pixel)

    private fun PrismTankFill.flatColor(): Int = when (this) {
        PrismTankFill.BLACK -> toArgb(255, 0, 0, 0)
        PrismTankFill.WHITE -> toArgb(255, 255, 255, 255)
        PrismTankFill.TRANSPARENT -> toArgb(0, 0, 0, 0)
        PrismTankFill.NEIGHBOR_AVG -> toArgb(255, 0, 0, 0)
    }

    private fun getAlpha(pixel: Int): Int = (pixel ushr 24) and 0xFF
    private fun getRed(pixel: Int): Int = (pixel ushr 16) and 0xFF
    private fun getGreen(pixel: Int): Int = (pixel ushr 8) and 0xFF
    private fun getBlue(pixel: Int): Int = pixel and 0xFF

    private fun toArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return ((alpha and 0xFF) shl 24) or
                ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                (blue and 0xFF)
    }
}