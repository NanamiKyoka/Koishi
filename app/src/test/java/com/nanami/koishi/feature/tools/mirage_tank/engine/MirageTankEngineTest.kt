package com.nanami.koishi.feature.tools.mirage_tank.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MirageTankEngineTest {

    private fun packArgb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    private fun getAlpha(p: Int): Int = (p ushr 24) and 0xFF
    private fun getRed(p: Int): Int = (p ushr 16) and 0xFF
    private fun getGreen(p: Int): Int = (p ushr 8) and 0xFF
    private fun getBlue(p: Int): Int = p and 0xFF

    @Test
    fun grayscale_pureWhiteFrontAndPureBlackBack_producesLowAlpha() {
        val width = 16
        val height = 16
        val frontPixels = IntArray(width * height) { packArgb(255, 255, 255, 255) } // 纯白表图
        val backPixels = IntArray(width * height) { packArgb(255, 0, 0, 0) }       // 纯黑里图

        val output = MirageTankEngine.processPixels(
            frontPixels = frontPixels,
            backPixels = backPixels,
            width = width,
            height = height,
            params = MirageTankParams(mode = MirageTankMode.GRAYSCALE)
        )

        // 验证每个像素的 Alpha 和色彩均在合法区间
        for (i in output.indices) {
            val pixel = output[i]
            val a = getAlpha(pixel)
            val r = getRed(pixel)
            val g = getGreen(pixel)
            val b = getBlue(pixel)

            assertTrue("Alpha 必须在 0..255 范围内", a in 0..255)
            assertEquals("黑白模式下 RGB 必须相等", r, g)
            assertEquals("黑白模式下 RGB 必须相等", g, b)
        }

        // 验证在白底与黑底下模拟渲染效果
        val whiteDisplay = MirageTankEngine.simulateDisplayPixels(output, width, height, 1.0f)
        val blackDisplay = MirageTankEngine.simulateDisplayPixels(output, width, height, 0.0f)

        val lumaWhite = getRed(whiteDisplay[0])
        val lumaBlack = getRed(blackDisplay[0])

        assertTrue("白底显示亮度应明显高于黑底显示亮度", lumaWhite > lumaBlack)
        assertTrue("白底显示应接近高亮", lumaWhite >= 200)
        assertTrue("黑底显示应接近暗部", lumaBlack <= 50)
    }

    @Test
    fun grayscale_arbitraryRandomPixels_keepsValuesClampedAndConsistent() {
        val random = Random(12345)
        val width = 32
        val height = 32
        val front = IntArray(width * height) {
            packArgb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
        val back = IntArray(width * height) {
            packArgb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }

        val result = MirageTankEngine.processPixels(
            frontPixels = front,
            backPixels = back,
            width = width,
            height = height,
            params = MirageTankParams(mode = MirageTankMode.GRAYSCALE)
        )

        val simWhite = MirageTankEngine.simulateDisplayPixels(result, width, height, 1.0f)
        val simBlack = MirageTankEngine.simulateDisplayPixels(result, width, height, 0.0f)

        for (i in result.indices) {
            val a = getAlpha(result[i])
            assertTrue("Alpha 范围溢出", a in 0..255)

            val rW = getRed(simWhite[i])
            val rB = getRed(simBlack[i])
            assertTrue("白底亮度应不小于黑底亮度", rW >= rB)
        }
    }

    @Test
    fun color_channelsAndBounds_areValid() {
        val width = 20
        val height = 20
        // 表图偏红，里图偏蓝
        val front = IntArray(width * height) { packArgb(255, 240, 60, 60) }
        val back = IntArray(width * height) { packArgb(255, 30, 40, 220) }

        val result = MirageTankEngine.processPixels(
            frontPixels = front,
            backPixels = back,
            width = width,
            height = height,
            params = MirageTankParams(mode = MirageTankMode.COLOR)
        )

        val simWhite = MirageTankEngine.simulateDisplayPixels(result, width, height, 1.0f)
        val simBlack = MirageTankEngine.simulateDisplayPixels(result, width, height, 0.0f)

        for (i in result.indices) {
            val p = result[i]
            val a = getAlpha(p)
            val r = getRed(p)
            val g = getGreen(p)
            val b = getBlue(p)

            assertTrue(a in 1..255)
            assertTrue(r in 0..255)
            assertTrue(g in 0..255)
            assertTrue(b in 0..255)

            // 黑底下应该能体现里图的蓝色特征
            val dispBlue = getBlue(simBlack[i])
            val dispRed = getRed(simBlack[i])
            assertTrue("黑底下里图的蓝色通道应占据优势", dispBlue >= dispRed)
        }
    }

    @Test
    fun checkerboard_createsSpatialAlternation() {
        val width = 4
        val height = 4
        val front = IntArray(width * height) { packArgb(255, 200, 200, 200) }
        val back = IntArray(width * height) { packArgb(255, 50, 50, 50) }

        val withCheckerboard = MirageTankEngine.processPixels(
            frontPixels = front,
            backPixels = back,
            width = width,
            height = height,
            params = MirageTankParams(
                mode = MirageTankMode.COLOR,
                enableCheckerboard = true,
                checkerboardStrength = 0.3f
            )
        )

        // 比较相邻像素 (0,0) 与 (1,0)，应该存在棋盘格扰动差异
        val p00 = withCheckerboard[0]
        val p10 = withCheckerboard[1]

        assertNotEquals("启用棋盘格时相邻点阵应产生空间交错差异", p00, p10)
    }

    @Test
    fun parameterExtremes_doNotCrashOrProduceNaN() {
        val width = 8
        val height = 8
        val front = IntArray(width * height) { packArgb(255, 128, 128, 128) }
        val back = IntArray(width * height) { packArgb(255, 128, 128, 128) }

        val extremeParams = listOf(
            MirageTankParams(frontLightness = 0.5f, backLightness = 1.5f),
            MirageTankParams(frontLightness = 1.5f, backLightness = 0.5f),
            MirageTankParams(frontContrast = 0.5f, backContrast = 1.5f),
            MirageTankParams(frontContrast = 1.5f, backContrast = 0.5f),
            MirageTankParams(enableCheckerboard = true, checkerboardStrength = 0.5f)
        )

        for (p in extremeParams) {
            val out = MirageTankEngine.processPixels(front, back, width, height, p)
            for (pixel in out) {
                assertTrue(getAlpha(pixel) in 0..255)
                assertTrue(getRed(pixel) in 0..255)
                assertTrue(getGreen(pixel) in 0..255)
                assertTrue(getBlue(pixel) in 0..255)
            }
        }
    }
}
