package com.nanami.koishi.feature.tools.image_stitching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.roundToInt

class ImageStitchingContractTest {

    @Test
    fun defaultCommonSettings_hasCorrectDefaults() {
        val settings = StitchingCommonSettings()
        assertEquals(1.0f, settings.scaleRatio, 0.001f)
        assertEquals(0, settings.gapPx)
        assertTrue(settings.autoScaleToMin)
        assertEquals(StitchBackgroundColor.WHITE, settings.backgroundColor)
        assertEquals(OutputFormat.PNG, settings.outputFormat)
    }

    @Test
    fun defaultSubtitleSettings_hasCorrectDefaults() {
        val settings = SubtitleSettings()
        assertEquals(0.72f, settings.cropRange.start, 0.001f)
        assertEquals(0.98f, settings.cropRange.endInclusive, 0.001f)
        assertEquals(false, settings.removeBlackBorders)
        assertEquals(true, settings.isCompressed)
        assertEquals(OutputFormat.JPEG, settings.outputFormat)
    }

    @Test
    fun verticalStitchingDimensions_autoScaleToMin() {
        val sizes = listOf(Pair(1000, 2000), Pair(800, 1200), Pair(1200, 1800))
        val autoScaleToMin = true
        val gap = 10
        val scaleRatio = 0.5f

        val targetWidth = if (autoScaleToMin) sizes.minOf { it.first } else sizes.maxOf { it.first }
        assertEquals(800, targetWidth)

        val scaledHeights = sizes.map { (w, h) ->
            val scale = targetWidth.toFloat() / w
            (h * scale).roundToInt()
        }
        assertEquals(1600, scaledHeights[0]) // 2000 * 0.8
        assertEquals(1200, scaledHeights[1]) // 1200 * 1.0
        assertEquals(1200, scaledHeights[2]) // 1800 * (800/1200) = 1200

        val totalHeightRaw = scaledHeights.sum() + (sizes.size - 1) * gap
        assertEquals(4020, totalHeightRaw)

        val finalWidth = (targetWidth * scaleRatio).roundToInt()
        val finalHeight = (totalHeightRaw * scaleRatio).roundToInt()
        assertEquals(400, finalWidth)
        assertEquals(2010, finalHeight)
    }

    @Test
    fun horizontalStitchingDimensions_autoScaleToMax() {
        val sizes = listOf(Pair(1000, 500), Pair(600, 400))
        val autoScaleToMin = false
        val gap = 20
        val scaleRatio = 1.0f

        val targetHeight = if (autoScaleToMin) sizes.minOf { it.second } else sizes.maxOf { it.second }
        assertEquals(500, targetHeight)

        val scaledWidths = sizes.map { (w, h) ->
            val scale = targetHeight.toFloat() / h
            (w * scale).roundToInt()
        }
        assertEquals(1000, scaledWidths[0])
        assertEquals(750, scaledWidths[1]) // 600 * (500/400) = 750

        val totalWidthRaw = scaledWidths.sum() + (sizes.size - 1) * gap
        assertEquals(1770, totalWidthRaw)

        val finalWidth = (totalWidthRaw * scaleRatio).roundToInt()
        val finalHeight = (targetHeight * scaleRatio).roundToInt()
        assertEquals(1770, finalWidth)
        assertEquals(500, finalHeight)
    }

    @Test
    fun subtitleStitchingDimensions_calculation() {
        val firstImage = Pair(1920, 1080)
        val subsequentImages = listOf(Pair(1920, 1080), Pair(1280, 720))
        val cropStart = 0.75f
        val cropEnd = 0.95f

        val targetWidth = firstImage.first
        val firstHeight = firstImage.second

        val slicesHeights = subsequentImages.map { (w, h) ->
            val srcTop = (h * cropStart).roundToInt()
            val srcBottom = (h * cropEnd).roundToInt()
            val srcH = srcBottom - srcTop
            val scale = targetWidth.toFloat() / w
            (srcH * scale).roundToInt()
        }

        // Image 2 (1920x1080): 1080 * 0.20 = 216
        assertEquals(216, slicesHeights[0])
        // Image 3 (1280x720): 720 * 0.20 = 144, scaled to 1920: 144 * (1920/1280) = 216
        assertEquals(216, slicesHeights[1])

        val totalHeight = firstHeight + slicesHeights.sum()
        assertEquals(1080 + 216 + 216, totalHeight)
        assertEquals(1512, totalHeight)
    }
}
