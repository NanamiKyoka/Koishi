package com.nanami.koishi.feature.tools.image_sketch.engine

import android.graphics.Bitmap
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 图片素描核心算法引擎
 *
 * 处理链路：灰度化 → 反相 → 高斯模糊 → 颜色减淡混合。
 * 设原始灰度图为 G，反相高斯模糊图为 B，最终素描值：
 * Sketch = min(255, G * 255 / (255 - B + 1e-5))
 */
object ImageSketchEngine {

    const val MIN_RADIUS = 1
    const val MAX_RADIUS = 100
    const val DEFAULT_RADIUS = 15

    private const val DIVISION_EPSILON = 1e-5
    private const val PARALLEL_WORK_THRESHOLD = 4_000_000L

    /**
     * 将用户选取的半径收敛到合法区间并保证为奇数，便于对称取样核构造。
     */
    fun normalizeRadius(radius: Int): Int {
        val coerced = radius.coerceIn(MIN_RADIUS, MAX_RADIUS)
        return if (coerced % 2 == 0) coerced + 1 else coerced
    }

    /**
     * 生成素描图，输出为不透明灰阶 ARGB_8888。
     */
    fun createSketch(source: Bitmap, radius: Int = DEFAULT_RADIUS): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val sketch = processPixels(pixels, width, height, radius)
        val output = IntArray(sketch.size) { index ->
            val value = sketch[index]
            packArgb(255, value, value, value)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(output, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 纯像素数组级完整链路（解耦 Android Bitmap，便于单元测试）。
     */
    fun processPixels(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val gray = toGrayscale(pixels)
        val inverted = invert(gray)
        val blurred = gaussianBlur(inverted, width, height, radius)
        return colorDodge(gray, blurred)
    }

    /**
     * 依据感知亮度加权 (0.299R + 0.587G + 0.114B) 将 ARGB 转为 0..255 灰度。
     */
    fun toGrayscale(pixels: IntArray): IntArray {
        val gray = IntArray(pixels.size)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val red = (pixel ushr 16) and 0xFF
            val green = (pixel ushr 8) and 0xFF
            val blue = pixel and 0xFF
            gray[index] = (0.299 * red + 0.587 * green + 0.114 * blue).roundToInt().coerceIn(0, 255)
        }
        return gray
    }

    /**
     * 反相处理：每个灰度值取 255 的补数。
     */
    fun invert(gray: IntArray): IntArray = IntArray(gray.size) { 255 - gray[it] }

    /**
     * 生成归一化一维高斯核，sigma 取半径的三分之一以覆盖约三个标准差。
     */
    fun buildGaussianKernel(radius: Int): FloatArray {
        val normalized = normalizeRadius(radius)
        val sigma = max(normalized / 3.0, 0.5)
        val size = normalized * 2 + 1
        val kernel = FloatArray(size)
        val denominator = 2.0 * sigma * sigma
        var sum = 0.0

        for (index in 0 until size) {
            val offset = (index - normalized).toDouble()
            val weight = exp(-(offset * offset) / denominator)
            kernel[index] = weight.toFloat()
            sum += weight
        }

        if (sum <= 0.0) {
            return FloatArray(size) { if (it == normalized) 1f else 0f }
        }
        for (index in 0 until size) {
            kernel[index] = (kernel[index] / sum).toFloat()
        }
        return kernel
    }

    /**
     * 可分离高斯模糊，横向与纵向各卷积一次，边界采用钳制延拓。
     * 每行之间互不依赖，因此按行切分到多核并行以摊薄大半径下的耗时。
     */
    fun gaussianBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        if (width <= 0 || height <= 0) return IntArray(0)

        val kernel = buildGaussianKernel(radius)
        val normalized = (kernel.size - 1) / 2
        val horizontal = IntArray(pixels.size)
        val result = IntArray(pixels.size)
        val workEstimate = pixels.size.toLong() * kernel.size

        forEachRowRange(height, workEstimate) { startY, endY ->
            for (y in startY until endY) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    var sum = 0f
                    for (tap in kernel.indices) {
                        val sampleX = (x + tap - normalized).coerceIn(0, width - 1)
                        sum += pixels[rowOffset + sampleX] * kernel[tap]
                    }
                    horizontal[rowOffset + x] = sum.roundToInt().coerceIn(0, 255)
                }
            }
        }

        forEachRowRange(height, workEstimate) { startY, endY ->
            for (y in startY until endY) {
                for (x in 0 until width) {
                    var sum = 0f
                    for (tap in kernel.indices) {
                        val sampleY = (y + tap - normalized).coerceIn(0, height - 1)
                        sum += horizontal[sampleY * width + x] * kernel[tap]
                    }
                    result[y * width + x] = sum.roundToInt().coerceIn(0, 255)
                }
            }
        }

        return result
    }

    private fun forEachRowRange(height: Int, workEstimate: Long, block: (Int, Int) -> Unit) {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val taskCount = minOf(coreCount, height)
        if (taskCount <= 1 || workEstimate < PARALLEL_WORK_THRESHOLD) {
            block(0, height)
            return
        }

        val step = ceil(height.toDouble() / taskCount).toInt()
        val tasks = ArrayList<Callable<Unit>>(taskCount)
        for (index in 0 until taskCount) {
            val start = index * step
            val end = (start + step).coerceAtMost(height)
            if (start >= end) break
            tasks.add(Callable { block(start, end) })
        }

        val executor = Executors.newFixedThreadPool(tasks.size)
        try {
            executor.invokeAll(tasks)
        } finally {
            executor.shutdown()
        }
    }

    /**
     * 颜色减淡混合，结果钳制在 0..255。
     * 当模糊底图达到满值 255 时直接输出 255，避免除零。
     */
    fun colorDodge(base: IntArray, blend: IntArray): IntArray {
        val size = minOf(base.size, blend.size)
        val output = IntArray(size)
        for (index in 0 until size) {
            val denominator = 255.0 - blend[index] + DIVISION_EPSILON
            output[index] = if (denominator <= DIVISION_EPSILON) {
                255
            } else {
                (base[index] * 255.0 / denominator).coerceIn(0.0, 255.0).roundToInt()
            }
        }
        return output
    }

    private fun packArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return ((alpha and 0xFF) shl 24) or
                ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                (blue and 0xFF)
    }
}
