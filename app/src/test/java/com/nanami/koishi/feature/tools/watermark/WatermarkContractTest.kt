package com.nanami.koishi.feature.tools.watermark

import com.nanami.koishi.feature.tools.watermark.engine.WatermarkConfig
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkFont
import com.nanami.koishi.feature.tools.watermark.engine.WatermarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkContractTest {

    @Test
    fun defaultUiState_hasExpectedDefaults() {
        val state = WatermarkUiState()

        assertNull(state.backgroundUri)
        assertNull(state.backgroundBitmap)
        assertNull(state.watermarkImageUri)
        assertNull(state.watermarkImageBitmap)
        assertNull(state.previewBitmap)
        assertFalse(state.isProcessing)
        assertFalse(state.isSaving)
        assertFalse(state.showColorPicker)
        assertNull(state.userMessage)

        val config = state.config
        assertEquals(WatermarkType.TEXT, config.type)
        assertEquals("Koishi Watermark", config.text)
        assertEquals(0.35f, config.alpha, 0.001f)
        assertEquals(-30f, config.rotation, 0.001f)
        assertEquals(120f, config.horizontalSpacing, 0.001f)
        assertEquals(100f, config.verticalSpacing, 0.001f)
        assertEquals(WatermarkFont.DEFAULT, config.font)
        assertNull(config.watermarkBitmap)
    }

    @Test
    fun configCopy_reflectsUpdates() {
        val original = WatermarkConfig()
        val updated = original.copy(
            type = WatermarkType.IMAGE,
            alpha = 0.8f,
            rotation = 45f,
            horizontalSpacing = 200f,
            verticalSpacing = 150f,
            imageScale = 1.5f
        )

        assertEquals(WatermarkType.IMAGE, updated.type)
        assertEquals(0.8f, updated.alpha, 0.001f)
        assertEquals(45f, updated.rotation, 0.001f)
        assertEquals(200f, updated.horizontalSpacing, 0.001f)
        assertEquals(150f, updated.verticalSpacing, 0.001f)
        assertEquals(1.5f, updated.imageScale, 0.001f)
    }

    @Test
    fun watermarkFont_containsAllStyles() {
        val fonts = WatermarkFont.entries
        assertEquals(5, fonts.size)
        assertTrue(fonts.contains(WatermarkFont.DEFAULT))
        assertTrue(fonts.contains(WatermarkFont.BOLD))
        assertTrue(fonts.contains(WatermarkFont.SERIF))
        assertTrue(fonts.contains(WatermarkFont.SANS_SERIF))
        assertTrue(fonts.contains(WatermarkFont.MONOSPACE))
    }
}
