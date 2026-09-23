package com.nanami.koishi.feature.tools.mirage_tank.engine

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 幻影坦克核心算法引擎
 *
 * 核心原理：
 * 设 PNG 输出像素为 (R, G, B, A)，背景灰度为 B_g (白底=255, 黑底=0)。
 * 渲染公式：C_out = C * (A / 255) + B_g * (1 - A / 255)
 *
 * 在白底 B_g=255 下渲染表图 W:
 * W = C * (A / 255) + 255 * (1 - A / 255)
 * 在黑底 B_g=0 下渲染里图 B:
 * B = C * (A / 255)
 *
 * 两式相减得：
 * W - B = 255 * (1 - A / 255) = 255 - A
 * => A = 255 - W + B
 * => C = B * 255 / A (当 A > 0 时)
 */
object MirageTankEngine {

    /**
     * 合成幻影坦克 Bitmap
     *
     * @param frontBitmap 表图（白底可见）
     * @param backBitmap 里图（黑底可见）
     * @param params 调优参数（模式、亮度、对比度、棋盘格调和等）
     * @return 带有精确 Alpha 通道的无损 ARGB_8888 格式 Bitmap
     */
    fun createMirageTank(
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        params: MirageTankParams = MirageTankParams()
    ): Bitmap {
        val targetWidth = max(frontBitmap.width, backBitmap.width)
        val targetHeight = max(frontBitmap.height, backBitmap.height)

        val scaledFront = if (frontBitmap.width == targetWidth && frontBitmap.height == targetHeight) {
            frontBitmap
        } else {
            Bitmap.createScaledBitmap(frontBitmap, targetWidth, targetHeight, true)
        }

        val scaledBack = if (backBitmap.width == targetWidth && backBitmap.height == targetHeight) {
            backBitmap
        } else {
            Bitmap.createScaledBitmap(backBitmap, targetWidth, targetHeight, true)
        }

        val frontPixels = IntArray(targetWidth * targetHeight)
        val backPixels = IntArray(targetWidth * targetHeight)
        scaledFront.getPixels(frontPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        scaledBack.getPixels(backPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        if (scaledFront != frontBitmap) scaledFront.recycle()
        if (scaledBack != backBitmap) scaledBack.recycle()

        val outputPixels = processPixels(
            frontPixels = frontPixels,
            backPixels = backPixels,
            width = targetWidth,
            height = targetHeight,
            params = params
        )

        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        return resultBitmap
    }

    /**
     * 纯像素数组级算法运算（解耦 Android Bitmap，便于高频处理与单元测试）
     */
    fun processPixels(
        frontPixels: IntArray,
        backPixels: IntArray,
        width: Int,
        height: Int,
        params: MirageTankParams = MirageTankParams()
    ): IntArray {
        val outputPixels = IntArray(width * height)
        when (params.mode) {
            MirageTankMode.GRAYSCALE -> {
                processGrayscale(
                    frontPixels = frontPixels,
                    backPixels = backPixels,
                    outputPixels = outputPixels,
                    width = width,
                    height = height,
                    params = params
                )
            }
            MirageTankMode.COLOR -> {
                processColor(
                    frontPixels = frontPixels,
                    backPixels = backPixels,
                    outputPixels = outputPixels,
                    width = width,
                    height = height,
                    params = params
                )
            }
        }
        return outputPixels
    }

    /**
     * 黑白幻影坦克算法实现
     */
    private fun processGrayscale(
        frontPixels: IntArray,
        backPixels: IntArray,
        outputPixels: IntArray,
        width: Int,
        height: Int,
        params: MirageTankParams
    ) {
        val fMin = 100f * params.frontLightness.coerceIn(0.5f, 1.5f)
        val bMax = 110f * params.backLightness.coerceIn(0.5f, 1.5f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pF = frontPixels[index]
                val pB = backPixels[index]

                // 计算灰度亮度 (标准感知加权亮度: 0.299R + 0.587G + 0.114B)
                val rawLumaF = 0.299f * getRed(pF) + 0.587f * getGreen(pF) + 0.114f * getBlue(pF)
                val rawLumaB = 0.299f * getRed(pB) + 0.587f * getGreen(pB) + 0.114f * getBlue(pB)

                // 对比度与亮度动态映射
                val adjLumaF = adjustContrast(rawLumaF, params.frontContrast)
                val adjLumaB = adjustContrast(rawLumaB, params.backContrast)

                // 表图映射到高亮区间 [fMin, 255]，里图映射到暗部区间 [0, bMax]
                var wVal = fMin + (adjLumaF / 255f) * (255f - fMin)
                var bVal = (adjLumaB / 255f) * bMax

                // 物理约束：白底亮度必须不小于黑底亮度 (W >= B) 以保证 Alpha <= 255
                if (wVal < bVal) {
                    val mid = (wVal + bVal) / 2f
                    wVal = mid
                    bVal = mid
                }

                // 棋盘格调和（若启用）
                if (params.enableCheckerboard) {
                    val dither = if ((x + y) % 2 == 0) params.checkerboardStrength * 16f else -params.checkerboardStrength * 16f
                    wVal = (wVal + dither).coerceIn(bVal, 255f)
                }

                // 解方程组：A = 255 - W + B
                val alphaFloat = (255f - wVal + bVal).coerceIn(0f, 255f)
                val alpha = alphaFloat.roundToInt()

                // 解像素颜色：C = B * 255 / A
                val gray = if (alpha <= 0) {
                    0
                } else {
                    ((bVal * 255f) / alphaFloat).roundToInt().coerceIn(0, 255)
                }

                outputPixels[index] = toArgb(alpha, gray, gray, gray)
            }
        }
    }

    /**
     * 彩色幻影坦克算法实现
     */
    private fun processColor(
        frontPixels: IntArray,
        backPixels: IntArray,
        outputPixels: IntArray,
        width: Int,
        height: Int,
        params: MirageTankParams
    ) {
        val fMin = 90f * params.frontLightness.coerceIn(0.5f, 1.5f)
        val bMax = 120f * params.backLightness.coerceIn(0.5f, 1.5f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pF = frontPixels[index]
                val pB = backPixels[index]

                // 分通道提取与对比度调整
                val rawRf = adjustContrast(getRed(pF).toFloat(), params.frontContrast)
                val rawGf = adjustContrast(getGreen(pF).toFloat(), params.frontContrast)
                val rawBf = adjustContrast(getBlue(pF).toFloat(), params.frontContrast)

                val rawRb = adjustContrast(getRed(pB).toFloat(), params.backContrast)
                val rawGb = adjustContrast(getGreen(pB).toFloat(), params.backContrast)
                val rawBb = adjustContrast(getBlue(pB).toFloat(), params.backContrast)

                // 范围映射 (表图亮化，里图暗化)
                var wR = fMin + (rawRf / 255f) * (255f - fMin)
                var wG = fMin + (rawGf / 255f) * (255f - fMin)
                var wB = fMin + (rawBf / 255f) * (255f - fMin)

                var bR = (rawRb / 255f) * bMax
                var bG = (rawGb / 255f) * bMax
                var bB = (rawBb / 255f) * bMax

                // 确保每通道 W >= B
                if (wR < bR) { val mid = (wR + bR) / 2f; wR = mid; bR = mid }
                if (wG < bG) { val mid = (wG + bG) / 2f; wG = mid; bG = mid }
                if (wB < bB) { val mid = (wB + bB) / 2f; wB = mid; bB = mid }

                // 计算三通道理想 Alpha
                val aR = 255f - wR + bR
                val aG = 255f - wG + bG
                val aB = 255f - wB + bB

                // 综合 Alpha (基于人眼亮度敏感度加权)
                var baseAlpha = 0.299f * aR + 0.587f * aG + 0.114f * aB

                // 棋盘格调和（空间交错优化色彩还原度）
                if (params.enableCheckerboard) {
                    val dither = if ((x + y) % 2 == 0) {
                        params.checkerboardStrength * 24f
                    } else {
                        -params.checkerboardStrength * 24f
                    }
                    baseAlpha = (baseAlpha + dither).coerceIn(1f, 255f)
                }

                val finalAlphaFloat = baseAlpha.coerceIn(1f, 255f)
                val finalAlpha = finalAlphaFloat.roundToInt().coerceIn(1, 255)

                // 计算各通道颜色值：C = B * 255 / Alpha
                val outR = ((bR * 255f) / finalAlphaFloat).roundToInt().coerceIn(0, 255)
                val outG = ((bG * 255f) / finalAlphaFloat).roundToInt().coerceIn(0, 255)
                val outB = ((bB * 255f) / finalAlphaFloat).roundToInt().coerceIn(0, 255)

                outputPixels[index] = toArgb(finalAlpha, outR, outG, outB)
            }
        }
    }

    /**
     * 对比度调节辅助算法 (中心点为 127.5)
     */
    private fun adjustContrast(value: Float, contrast: Float): Float {
        val factor = contrast.coerceIn(0.5f, 1.5f)
        return ((value - 127.5f) * factor + 127.5f).coerceIn(0f, 255f)
    }

    /**
     * 模拟幻影坦克在特定背景灰度下的显示效果像素数组
     */
    fun simulateDisplayPixels(
        tankPixels: IntArray,
        width: Int,
        height: Int,
        backgroundBrightness: Float
    ): IntArray {
        val bgVal = (backgroundBrightness.coerceIn(0f, 1f) * 255f).roundToInt()
        val outPixels = IntArray(width * height)

        for (i in tankPixels.indices) {
            val pixel = tankPixels[i]
            val a = getAlpha(pixel) / 255f
            val r = getRed(pixel)
            val g = getGreen(pixel)
            val b = getBlue(pixel)

            val outR = (r * a + bgVal * (1f - a)).roundToInt().coerceIn(0, 255)
            val outG = (g * a + bgVal * (1f - a)).roundToInt().coerceIn(0, 255)
            val outB = (b * a + bgVal * (1f - a)).roundToInt().coerceIn(0, 255)

            outPixels[i] = toArgb(255, outR, outG, outB)
        }
        return outPixels
    }

    /**
     * 模拟幻影坦克在特定背景灰度下的显示效果
     *
     * @param tankBitmap 带有透明通道的幻影坦克图片
     * @param backgroundBrightness 背景亮度比例（0.0f = 纯黑底, 1.0f = 纯白底）
     * @return 混合后的实际显示像素数组 (ARGB_8888)
     */
    fun simulateDisplay(tankBitmap: Bitmap, backgroundBrightness: Float): Bitmap {
        val width = tankBitmap.width
        val height = tankBitmap.height
        val pixels = IntArray(width * height)
        tankBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = simulateDisplayPixels(pixels, width, height, backgroundBrightness)
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return outBitmap
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
