package com.nanami.koishi.core.image.crop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImageAdjustmentEngineTest {

    @Test
    fun testDefaultAdjustmentsNoFilter() {
        // 当亮度、对比度、饱和度均为 0 时，无需生成 ColorFilter
        val filter = ImageAdjustmentEngine.createComposeColorFilter(0f, 0f, 0f)
        assertNull("默认调节参数下无需分配 ColorFilter", filter)
    }

    @Test
    fun testActiveAdjustmentsCreatesFilter() {
        val filterBrightness = ImageAdjustmentEngine.createComposeColorFilter(20f, 0f, 0f)
        assertNotNull("亮度调整时应生成 ColorFilter", filterBrightness)

        val filterContrast = ImageAdjustmentEngine.createComposeColorFilter(0f, -15f, 0f)
        assertNotNull("对比度调整时应生成 ColorFilter", filterContrast)

        val filterSaturation = ImageAdjustmentEngine.createComposeColorFilter(0f, 0f, 50f)
        assertNotNull("饱和度调整时应生成 ColorFilter", filterSaturation)
    }

    @Test
    fun testColorMatrixArrayLength() {
        val array = ImageAdjustmentEngine.createColorMatrixArray(10f, 20f, -30f)
        assertNotNull(array)
        assertEquals("色彩矩阵必须正好包含 20 个 Float 元素", 20, array.size)
    }

    @Test
    fun testIdentityMultiplication() {
        val identity = ImageAdjustmentEngine.identityMatrix()
        val custom = ImageAdjustmentEngine.createColorMatrixArray(15f, 0f, 0f)
        val result = ImageAdjustmentEngine.multiplyColorMatrices(custom, identity)
        for (i in 0 until 20) {
            assertEquals(custom[i], result[i], 0.001f)
        }
    }
}
