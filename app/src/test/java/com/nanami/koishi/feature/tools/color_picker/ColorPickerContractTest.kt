package com.nanami.koishi.feature.tools.color_picker

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerContractTest {

    @Test
    fun `ColorPickerUiState computes correct hex and rgb strings`() {
        val state = ColorPickerUiState(
            selectedColor = Color(0xFFF5B5C0),
            cursorX = 911,
            cursorY = 463
        )

        assertEquals("#F5B5C0", state.hexString)
        assertEquals("245, 181, 192", state.rgbString)
        assertEquals("X911, Y463", state.pixelCoordinateString)
        assertEquals(245, state.redInt)
        assertEquals(181, state.greenInt)
        assertEquals(192, state.blueInt)
    }

    @Test
    fun `ColorPickerUiState default color is pure white`() {
        val defaultState = ColorPickerUiState()
        assertEquals(Color(0xFFFFFFFF), defaultState.selectedColor)
        assertEquals("#FFFFFF", defaultState.hexString)
        assertEquals("255, 255, 255", defaultState.rgbString)
    }

    @Test
    fun `ColorPickerUiState handles black and white colors`() {
        val black = ColorPickerUiState(selectedColor = Color(0xFF000000))
        assertEquals("#000000", black.hexString)
        assertEquals("0, 0, 0", black.rgbString)

        val white = ColorPickerUiState(selectedColor = Color(0xFFFFFFFF))
        assertEquals("#FFFFFF", white.hexString)
        assertEquals("255, 255, 255", white.rgbString)
    }
}
