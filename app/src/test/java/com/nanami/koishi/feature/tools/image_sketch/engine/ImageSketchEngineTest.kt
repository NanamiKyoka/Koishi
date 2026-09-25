package com.nanami.koishi.feature.tools.image_sketch.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ImageSketchEngineTest {

    private fun packArgb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    private fun grayPixels(width: Int, height: Int, gray: Int): IntArray {
        return IntArray(width * height) { packArgb(255, gray, gray, gray) }
    }

    @Test
    fun toGrayscale_usesPerceptualLumaWeights() {
        val pixels = intArrayOf(
            packArgb(255, 255, 255, 255),
            packArgb(255, 0, 0, 0),
            packArgb(255, 255, 0, 0),
            packArgb(255, 0, 255, 0),
            packArgb(255, 0, 0, 255)
        )

        val gray = ImageSketchEngine.toGrayscale(pixels)

        assertEquals(255, gray[0])
        assertEquals(0, gray[1])
        assertEquals(76, gray[2])
        assertEquals(150, gray[3])
        assertEquals(29, gray[4])
    }

    @Test
    fun invert_isComplementTo255() {
        val gray = intArrayOf(0, 1, 128, 254, 255)
        val inverted = ImageSketchEngine.invert(gray)

        assertEquals(255, inverted[0])
        assertEquals(254, inverted[1])
        assertEquals(127, inverted[2])
        assertEquals(1, inverted[3])
        assertEquals(0, inverted[4])
    }

    @Test
    fun normalizeRadius_keepsOddAndPromotesEvenWithinRange() {
        assertEquals(15, ImageSketchEngine.normalizeRadius(15))
        assertEquals(1, ImageSketchEngine.normalizeRadius(1))
        assertEquals(49, ImageSketchEngine.normalizeRadius(49))
        assertEquals(99, ImageSketchEngine.normalizeRadius(99))
        assertEquals(17, ImageSketchEngine.normalizeRadius(16))
        assertEquals(3, ImageSketchEngine.normalizeRadius(2))
        assertEquals(51, ImageSketchEngine.normalizeRadius(50))
        assertEquals(101, ImageSketchEngine.normalizeRadius(100))
        assertEquals(1, ImageSketchEngine.normalizeRadius(-5))
        assertEquals(101, ImageSketchEngine.normalizeRadius(200))
    }

    @Test
    fun buildGaussianKernel_isNormalizedSymmetricAndCentered() {
        val kernel = ImageSketchEngine.buildGaussianKernel(15)

        assertEquals(31, kernel.size)

        var sum = 0f
        for (value in kernel) sum += value
        assertEquals(1f, sum, 1e-4f)

        val center = kernel.size / 2
        for (offset in 1..center) {
            assertEquals(kernel[center - offset], kernel[center + offset], 1e-6f)
        }
        for (index in kernel.indices) {
            assertTrue(kernel[index] <= kernel[center])
        }
    }

    @Test
    fun gaussianBlur_keepsConstantFieldStable() {
        val width = 16
        val height = 16
        val pixels = IntArray(width * height) { 120 }

        val blurred = ImageSketchEngine.gaussianBlur(pixels, width, height, 9)

        for (value in blurred) {
            assertEquals(120, value)
        }
    }

    @Test
    fun gaussianBlur_softensIsolatedSpike() {
        val width = 21
        val height = 21
        val pixels = IntArray(width * height)
        val centerIndex = 10 * width + 10
        pixels[centerIndex] = 255

        val blurred = ImageSketchEngine.gaussianBlur(pixels, width, height, 5)

        val center = blurred[centerIndex]
        assertTrue("中心应保留最高响应", center > 0 && center < 255)
        assertTrue("中心响应应高于相邻点", center > blurred[centerIndex + 1])
        assertTrue("相邻点响应应高于更远处", blurred[centerIndex + 1] > blurred[centerIndex + 3])
        assertEquals(0, blurred[0])
    }

    @Test
    fun gaussianBlur_matchesNaiveTwoDimensionalReference() {
        val width = 12
        val height = 9
        val radius = 3
        val pixels = IntArray(width * height) { (it * 91) % 256 }
        val kernel = ImageSketchEngine.buildGaussianKernel(radius)
        val half = (kernel.size - 1) / 2

        val expected = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0.0
                for (ty in kernel.indices) {
                    for (tx in kernel.indices) {
                        val sampleY = (y + ty - half).coerceIn(0, height - 1)
                        val sampleX = (x + tx - half).coerceIn(0, width - 1)
                        sum += pixels[sampleY * width + sampleX] * (kernel[ty] * kernel[tx])
                    }
                }
                expected[y * width + x] = sum.roundToInt().coerceIn(0, 255)
            }
        }

        val actual = ImageSketchEngine.gaussianBlur(pixels, width, height, radius)

        for (index in actual.indices) {
            assertTrue(
                "可分离卷积应与二维参考一致（仅允许舍入误差）",
                kotlin.math.abs(actual[index] - expected[index]) <= 2
            )
        }
    }

    @Test
    fun gaussianBlur_parallelScaleIsDeterministicAndClamped() {
        val width = 512
        val height = 512
        val pixels = IntArray(width * height) { (it * 53) % 256 }

        val first = ImageSketchEngine.gaussianBlur(pixels, width, height, 15)
        val second = ImageSketchEngine.gaussianBlur(pixels, width, height, 15)

        assertTrue("多核按行切分不得产生竞态差异", first.contentEquals(second))
        for (value in first) {
            assertTrue(value in 0..255)
        }
    }

    @Test
    fun colorDodge_matchesFormulaAndGuardsDivisionByZero() {
        val base = intArrayOf(0, 0, 200, 255, 100)
        val blend = intArrayOf(255, 0, 50, 254, 200)

        val dodge = ImageSketchEngine.colorDodge(base, blend)

        assertEquals(255, dodge[0])
        assertEquals(0, dodge[1])
        assertEquals(249, dodge[2])
        assertEquals(255, dodge[3])
        assertEquals(255, dodge[4])
    }

    @Test
    fun colorDodge_neverExceedsByteRangeForRandomInput() {
        val random = kotlin.random.Random(20240925)
        val base = IntArray(256) { random.nextInt(0, 256) }
        val blend = IntArray(256) { random.nextInt(0, 256) }

        val dodge = ImageSketchEngine.colorDodge(base, blend)

        for (value in dodge) {
            assertTrue(value in 0..255)
        }
    }

    @Test
    fun processPixels_flatImagesConvergeToWhite() {
        val width = 24
        val height = 24
        for (gray in intArrayOf(0, 40, 128, 200, 255)) {
            val output = ImageSketchEngine.processPixels(
                pixels = grayPixels(width, height, gray),
                width = width,
                height = height,
                radius = ImageSketchEngine.DEFAULT_RADIUS
            )
            for (value in output) {
                assertEquals("平坦区域应被减淡为纯白", 255, value)
            }
        }
    }

    @Test
    fun processPixels_stepEdgeProducesDarkLineAndWhiteField() {
        val width = 24
        val height = 4
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            if (x < width / 2) packArgb(255, 255, 255, 255) else packArgb(255, 0, 0, 0)
        }

        val output = ImageSketchEngine.processPixels(pixels, width, height, 3)

        val firstRow = IntArray(width) { output[it] }
        assertTrue("画面左侧平坦白色区域应保持纯白", firstRow.first() == 255)
        assertTrue("画面右侧平坦深色区域应被减淡为纯白", firstRow.last() == 255)

        val darkest = firstRow.min()
        assertTrue("交界处应勾勒出明显深色线条", darkest < 120)
    }

    @Test
    fun processPixels_evenRadiusMatchesPromotedOddRadius() {
        val width = 20
        val height = 20
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            val value = ((x * 255) / (width - 1) + (y * 255) / (height - 1)) / 2
            packArgb(255, value, value, value)
        }

        val even = ImageSketchEngine.processPixels(pixels, width, height, 16)
        val odd = ImageSketchEngine.processPixels(pixels, width, height, 17)

        assertTrue(even.contentEquals(odd))
    }

    @Test
    fun processPixels_extremeRadiiStayWithinByteRange() {
        val width = 32
        val height = 32
        val pixels = IntArray(width * height) { index ->
            val value = (index * 37) % 256
            packArgb(255, value, value, value)
        }

        for (radius in intArrayOf(ImageSketchEngine.MIN_RADIUS, 16, ImageSketchEngine.MAX_RADIUS)) {
            val output = ImageSketchEngine.processPixels(pixels, width, height, radius)
            assertEquals(width * height, output.size)
            for (value in output) {
                assertTrue("素描输出必须落在 0..255", value in 0..255)
            }
        }
    }

    @Test
    fun processPixels_zeroSizedInputReturnsEmpty() {
        val output = ImageSketchEngine.processPixels(IntArray(0), 0, 0, 15)
        assertEquals(0, output.size)
    }
}
