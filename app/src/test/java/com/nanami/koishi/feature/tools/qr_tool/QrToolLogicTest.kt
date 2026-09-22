package com.nanami.koishi.feature.tools.qr_tool

import com.nanami.koishi.feature.tools.qr_tool.components.QrThemePresets
import com.nanami.koishi.feature.tools.qr_tool.engine.QrConfig
import com.nanami.koishi.feature.tools.qr_tool.engine.QrDotStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import qrcode.raw.ErrorCorrectionLevel
import qrcode.raw.QRCodeProcessor

class QrToolLogicTest {

    @Test
    fun testQrEncodingMatrixGeneration() {
        val testContent = "https://github.com/nanami"
        val processor = QRCodeProcessor(
            data = testContent,
            errorCorrectionLevel = ErrorCorrectionLevel.MEDIUM
        )
        val rawData = processor.encode()

        assertTrue("二维码矩阵尺寸必须大于 20", rawData.size >= 21)
        assertNotNull(rawData[0][0])
        // 四个角之一必有定位点
        assertTrue("左上角必须有定位深色块", rawData[0][0].dark)
    }

    @Test
    fun testThemePresetsIntegrity() {
        val presets = QrThemePresets.presets
        assertEquals("应包含 8 种精选主题配色", 8, presets.size)

        // 验证预设包含抹茶绿和经典玄黑
        val matcha = presets.find { it.id == "matcha" }
        assertNotNull("必须包含抹茶主题", matcha)

        val black = presets.find { it.id == "black" }
        assertNotNull("必须包含玄黑主题", black)
    }

    @Test
    fun testQrConfigDefaults() {
        val config = QrConfig()
        assertEquals(QrDotStyle.SQUARE, config.dotStyle)
        assertTrue(config.dotScale in 0.2f..1.0f)
        assertFalse(config.isPickFromBg)
    }
}
