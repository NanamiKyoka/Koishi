package com.nanami.koishi.feature.tools.video_to_gif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoToGifContractTest {

    @Test
    fun testResolutionScaleLandscape() {
        val originalW = 1920
        val originalH = 1080

        val (w720, h720) = ResolutionScale.P720.calculateTargetDimensions(originalW, originalH)
        assertEquals(720, h720)
        assertEquals(1280, w720)
        assertEquals(0, w720 % 2)
        assertEquals(0, h720 % 2)

        val (w480, h480) = ResolutionScale.P480.calculateTargetDimensions(originalW, originalH)
        assertEquals(480, h480)
        assertEquals(852, w480)
        assertEquals(0, w480 % 2)
        assertEquals(0, h480 % 2)

        val (w360, h360) = ResolutionScale.P360.calculateTargetDimensions(originalW, originalH)
        assertEquals(360, h360)
        assertEquals(640, w360)
        assertEquals(0, w360 % 2)
        assertEquals(0, h360 % 2)
    }

    @Test
    fun testResolutionScalePortrait() {
        val originalW = 1080
        val originalH = 1920

        val (w720, h720) = ResolutionScale.P720.calculateTargetDimensions(originalW, originalH)
        assertEquals(720, w720)
        assertEquals(1280, h720)
        assertEquals(0, w720 % 2)
        assertEquals(0, h720 % 2)

        val (w480, h480) = ResolutionScale.P480.calculateTargetDimensions(originalW, originalH)
        assertEquals(480, w480)
        assertEquals(852, h480)
        assertEquals(0, w480 % 2)
        assertEquals(0, h480 % 2)
    }

    @Test
    fun testResolutionScaleOriginalDimensions() {
        val (wSmall, hSmall) = ResolutionScale.ORIGINAL.calculateTargetDimensions(800, 600)
        assertEquals(800, wSmall)
        assertEquals(600, hSmall)

        val (wBig, hBig) = ResolutionScale.ORIGINAL.calculateTargetDimensions(3840, 2160)
        assertTrue(wBig <= 1280)
        assertTrue(hBig <= 1280)
        assertEquals(0, wBig % 2)
        assertEquals(0, hBig % 2)
    }

    @Test
    fun testUiStateCalculatedProperties() {
        val emptyState = VideoToGifUiState()
        assertFalse(emptyState.hasVideo)
        assertFalse(emptyState.canConvert)
        assertEquals(0L, emptyState.trimDurationMs)

        val readyState = VideoToGifUiState(
            videoUri = android.net.FakeUri(),
            videoDurationMs = 10000L,
            startTrimMs = 1000L,
            endTrimMs = 4000L,
            selectedFps = 15
        )
        assertTrue(readyState.hasVideo)
        assertTrue(readyState.canConvert)
        assertEquals(3000L, readyState.trimDurationMs)
        assertEquals(45, readyState.estimatedFrameCount)

        val processingState = readyState.copy(isProcessing = true)
        assertFalse(processingState.canConvert)

        val tooShortState = readyState.copy(startTrimMs = 1000L, endTrimMs = 1050L)
        assertFalse(tooShortState.canConvert)
    }

    @Test
    fun testNeuQuantColorQuantization() {
        val bgr = ByteArray(1000 * 3) { idx -> ((idx * 17) % 256).toByte() }
        val quantizer = com.nanami.koishi.feature.tools.video_to_gif.engine.AnimatedGifEncoder.NeuQuant(
            bgr,
            bgr.size,
            10
        )
        val palette = quantizer.process()
        assertEquals(256 * 3, palette.size)

        val mappedIndex = quantizer.map(100, 150, 200)
        assertTrue(mappedIndex in 0..255)
    }

    @Test
    fun testLzwEncoderEncoding() {
        val width = 200
        val height = 150
        val pixels = ByteArray(width * height) { idx -> (idx % 256).toByte() }

        val bos = java.io.ByteArrayOutputStream()
        val encoder = com.nanami.koishi.feature.tools.video_to_gif.engine.AnimatedGifEncoder.LzwEncoder(
            width,
            height,
            pixels,
            8
        )
        encoder.encode(bos)

        val output = bos.toByteArray()
        assertTrue(output.isNotEmpty())
        assertEquals(8, output[0].toInt() and 0xFF)
        assertEquals(0, output.last().toInt() and 0xFF)
        assertTrue(output.size > 100)
    }
}
