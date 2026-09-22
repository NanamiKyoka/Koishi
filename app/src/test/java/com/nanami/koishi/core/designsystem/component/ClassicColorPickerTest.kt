package com.nanami.koishi.core.designsystem.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClassicColorPickerTest {

    @Test
    fun testFormatHexOpaque() {
        val red = Color(1f, 0f, 0f, 1f)
        assertEquals("#FF0000", formatHex(red))

        val white = Color(1f, 1f, 1f, 1f)
        assertEquals("#FFFFFF", formatHex(white))

        val black = Color(0f, 0f, 0f, 1f)
        assertEquals("#000000", formatHex(black))
    }

    @Test
    fun testFormatHexTransparent() {
        val semiGreen = Color(0f, 1f, 0f, 0.5f)
        // 0.5f * 255 = 127.5 -> 128 (0x80)
        assertEquals("#8000FF00", formatHex(semiGreen))
    }

    @Test
    fun testParseHexColorSixDigits() {
        val color = parseHexColor("#FF5722")
        assertNotNull(color)
        assertEquals(1f, color!!.alpha, 0.01f)
        assertEquals(1f, color.red, 0.01f)
        assertEquals(87f / 255f, color.green, 0.01f)
        assertEquals(34f / 255f, color.blue, 0.01f)

        val noHashColor = parseHexColor("00FF00")
        assertNotNull(noHashColor)
        assertEquals(1f, noHashColor!!.green, 0.01f)
    }

    @Test
    fun testParseHexColorEightDigits() {
        val color = parseHexColor("#800000FF")
        assertNotNull(color)
        assertEquals(128f / 255f, color!!.alpha, 0.01f)
        assertEquals(0f, color.red, 0.01f)
        assertEquals(0f, color.green, 0.01f)
        assertEquals(1f, color.blue, 0.01f)
    }

    @Test
    fun testParseHexColorInvalid() {
        assertNull(parseHexColor("#XYZ"))
        assertNull(parseHexColor("12345"))
        assertNull(parseHexColor("#1234567"))
    }
}
