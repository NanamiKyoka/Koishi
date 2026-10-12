package com.nanami.koishi.feature.tools.prism_tank.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class PrismTankEngineTest {

    private fun packArgb(a: Int, r: Int, g: Int, b: Int): Int =
        ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    private fun alphaOf(pixel: Int): Int = (pixel ushr 24) and 0xFF
    private fun redOf(pixel: Int): Int = (pixel ushr 16) and 0xFF
    private fun greenOf(pixel: Int): Int = (pixel ushr 8) and 0xFF
    private fun blueOf(pixel: Int): Int = pixel and 0xFF

    private fun lumaOf(pixel: Int): Float =
        0.299f * redOf(pixel) + 0.587f * greenOf(pixel) + 0.114f * blueOf(pixel)

    private fun constantPixels(width: Int, height: Int, pixel: Int) = IntArray(width * height) { pixel }

    @Test
    fun defaultParams_produceDiagonalStripePattern() {
        val params = PrismTankParams()

        assertTrue(PrismTankEngine.isCoverPixel(0, 0, params))
        assertTrue(PrismTankEngine.isCoverPixel(1, 1, params))
        assertTrue(PrismTankEngine.isCoverPixel(0, 2, params))
        assertFalse(PrismTankEngine.isCoverPixel(1, 0, params))
        assertFalse(PrismTankEngine.isCoverPixel(0, 1, params))
    }

    @Test
    fun encode_keepsInnerAndCoverInDisjointBrightnessBands() {
        val width = 32
        val height = 32
        val random = Random(20241012)
        val inner = IntArray(width * height) {
            packArgb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
        val cover = IntArray(width * height) {
            packArgb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
        val params = PrismTankParams()

        val output = PrismTankEngine.encodePixels(inner, cover, width, height, params)

        val innerThreshold = params.usableInnerThreshold
        val coverThreshold = params.usableCoverThreshold
        for (index in output.indices) {
            val pixel = output[index]
            assertEquals("输出必须不透明", 255, alphaOf(pixel))
            val luma = lumaOf(pixel)
            assertTrue(
                "像素亮度 $luma 必须落在里图区间或表图区间",
                luma <= innerThreshold + 1f || luma >= coverThreshold - 1f
            )
        }
    }

    @Test
    fun encode_reverseMode_keepsInnerLighterThanCover() {
        val width = 16
        val height = 16
        val inner = constantPixels(width, height, packArgb(255, 255, 255, 255))
        val cover = constantPixels(width, height, packArgb(255, 0, 0, 0))
        val params = PrismTankParams(isReverse = true)

        val output = PrismTankEngine.encodePixels(inner, cover, width, height, params)

        val innerThreshold = params.usableInnerThreshold
        val coverThreshold = params.usableCoverThreshold
        for (pixel in output) {
            val luma = lumaOf(pixel)
            assertTrue(
                "反相模式下里图必须在亮部、表图必须在暗部",
                luma >= 255 - innerThreshold - 1f || luma <= 255 - coverThreshold + 1f
            )
        }
    }

    @Test
    fun revealInner_recoversHiddenImageFromFlatColorTank() {
        val width = 24
        val height = 24
        val innerColor = packArgb(255, 120, 60, 200)
        val coverColor = packArgb(255, 200, 200, 200)
        val params = PrismTankParams(innerThreshold = 96, coverThreshold = 120)

        val tank = PrismTankEngine.encodePixels(
            innerPixels = constantPixels(width, height, innerColor),
            coverPixels = constantPixels(width, height, coverColor),
            width = width,
            height = height,
            params = params
        )

        val revealed = PrismTankEngine.revealInnerPixels(
            pixels = tank,
            width = width,
            height = height,
            config = PrismTankRevealConfig(higherThreshold = params.usableInnerThreshold)
        )

        for (pixel in revealed) {
            assertTrue(abs(redOf(pixel) - 120) <= 4)
            assertTrue(abs(greenOf(pixel) - 60) <= 4)
            assertTrue(abs(blueOf(pixel) - 200) <= 4)
        }
    }

    @Test
    fun revealInner_withBlackFill_leavesCoverPixelsBlack() {
        val width = 16
        val height = 16
        val innerColor = packArgb(255, 200, 30, 30)
        val coverColor = packArgb(255, 10, 10, 10)
        val params = PrismTankParams()

        val tank = PrismTankEngine.encodePixels(
            innerPixels = constantPixels(width, height, innerColor),
            coverPixels = constantPixels(width, height, coverColor),
            width = width,
            height = height,
            params = params
        )

        val revealed = PrismTankEngine.revealInnerPixels(
            pixels = tank,
            width = width,
            height = height,
            config = PrismTankRevealConfig(fill = PrismTankFill.BLACK)
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = revealed[y * width + x]
                if (PrismTankEngine.isCoverPixel(x, y, params)) {
                    assertEquals(packArgb(255, 0, 0, 0), pixel)
                }
            }
        }
    }

    @Test
    fun revealCover_removesInnerStripes() {
        val width = 16
        val height = 16
        val innerColor = packArgb(255, 255, 255, 255)
        val coverColor = packArgb(255, 128, 128, 128)
        val params = PrismTankParams(innerThreshold = 20, coverThreshold = 90)

        val tank = PrismTankEngine.encodePixels(
            innerPixels = constantPixels(width, height, innerColor),
            coverPixels = constantPixels(width, height, coverColor),
            width = width,
            height = height,
            params = params
        )

        val revealed = PrismTankEngine.revealCoverPixels(
            pixels = tank,
            width = width,
            height = height,
            config = PrismTankRevealConfig(higherThreshold = params.usableInnerThreshold)
        )

        val expectedCover = 90 + (128 * 165) / 255
        for (pixel in revealed) {
            assertTrue(abs(redOf(pixel) - expectedCover) <= 1)
            assertTrue(abs(greenOf(pixel) - expectedCover) <= 1)
            assertTrue(abs(blueOf(pixel) - expectedCover) <= 1)
        }
    }

    @Test
    fun applyExposure_brightensDarkPixelsAndClipsBrightOnes() {
        val pixels = intArrayOf(
            packArgb(255, 0, 0, 0),
            packArgb(255, 20, 20, 20),
            packArgb(255, 128, 128, 128),
            packArgb(255, 255, 255, 255)
        )

        val exposed = PrismTankEngine.applyExposurePixels(pixels, gain = 10f)

        assertEquals(packArgb(255, 0, 0, 0), exposed[0])
        assertEquals(packArgb(255, 200, 200, 200), exposed[1])
        assertEquals(packArgb(255, 255, 255, 255), exposed[2])
        assertEquals(packArgb(255, 255, 255, 255), exposed[3])
    }

    @Test
    fun encode_respectsGapPattern() {
        val params = PrismTankParams(gap = 2, slope = 1, isRow = true)

        val coverCount = (0 until 12).count { PrismTankEngine.isCoverPixel(it, 0, params) }
        assertEquals("gap=2 时每 3 个像素应有 2 个表图像素", 8, coverCount)
    }
}