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

    @Test
    fun testCalculateBaseScaleCoversCropBox() {
        val cropSide = 800f

        // 1. 正方形图片
        val squareScale = ImageAdjustmentEngine.calculateBaseScale(
            sourceWidth = 1000f,
            sourceHeight = 1000f,
            cropBoxWidth = cropSide,
            cropBoxHeight = cropSide
        )
        assertEquals(0.8f, squareScale, 0.001f)

        // 2. 竖屏图片 (宽较窄)：应按宽度缩放，保证宽度填满裁剪框，高度溢出
        val portraitScale = ImageAdjustmentEngine.calculateBaseScale(
            sourceWidth = 500f,
            sourceHeight = 1000f,
            cropBoxWidth = cropSide,
            cropBoxHeight = cropSide
        )
        assertEquals(1.6f, portraitScale, 0.001f)
        assertEquals(800f, 500f * portraitScale, 0.001f) // 宽度刚好为 800
        assertEquals(1600f, 1000f * portraitScale, 0.001f) // 高度溢出裁剪框

        // 3. 横屏图片 (高较矮)：应按高度缩放，保证高度填满裁剪框，宽度溢出
        val landscapeScale = ImageAdjustmentEngine.calculateBaseScale(
            sourceWidth = 1200f,
            sourceHeight = 600f,
            cropBoxWidth = cropSide,
            cropBoxHeight = cropSide
        )
        assertEquals(1.3333f, landscapeScale, 0.001f)
        assertEquals(800f, 600f * landscapeScale, 0.001f) // 高度刚好为 800
        assertEquals(1600f, 1200f * landscapeScale, 0.001f) // 宽度溢出裁剪框
    }

    @Test
    fun testClampPanAndScale_whenUnconstrained_allowsFreePanAndScale() {
        val (pan, scale) = ImageAdjustmentEngine.clampPanAndScale(
            pan = androidx.compose.ui.geometry.Offset(500f, -800f),
            scale = 0.6f,
            rotationDegrees = 0f,
            sourceWidth = 1000f,
            sourceHeight = 1000f,
            cropBoxWidth = 800f,
            cropBoxHeight = 800f,
            constrainToImage = false
        )
        assertEquals(0.6f, scale, 0.001f)
        assertEquals(500f, pan.x, 0.001f)
        assertEquals(-800f, pan.y, 0.001f)
    }

    @Test
    fun testClampPanAndScale_whenConstrained_clampsScaleAndPan() {
        // 1. 缩放比例小于 1.0 时强制纠正为至少 1.0 (0度无旋转)
        val (pan1, scale1) = ImageAdjustmentEngine.clampPanAndScale(
            pan = androidx.compose.ui.geometry.Offset(0f, 0f),
            scale = 0.7f,
            rotationDegrees = 0f,
            sourceWidth = 1000f,
            sourceHeight = 1000f,
            cropBoxWidth = 800f,
            cropBoxHeight = 800f,
            constrainToImage = true
        )
        assertEquals(1.0f, scale1, 0.001f)
        assertEquals(0f, pan1.x, 0.001f)
        assertEquals(0f, pan1.y, 0.001f)

        // 2. 缩放到 1.5 倍时，drawW = 800 * 1.5 = 1200，允许的最大平移为 (1200 - 800) / 2 = 200
        val (pan2, scale2) = ImageAdjustmentEngine.clampPanAndScale(
            pan = androidx.compose.ui.geometry.Offset(450f, -350f),
            scale = 1.5f,
            rotationDegrees = 0f,
            sourceWidth = 1000f,
            sourceHeight = 1000f,
            cropBoxWidth = 800f,
            cropBoxHeight = 800f,
            constrainToImage = true
        )
        assertEquals(1.5f, scale2, 0.001f)
        assertEquals(200f, pan2.x, 0.001f)
        assertEquals(-200f, pan2.y, 0.001f)

        // 3. 旋转 90 度时，竖屏图片 500x1000，基础宽高 drawW=800, drawH=1600
        // 旋转 90 度后，原图的宽(800)转到了纵向，高(1600)转到了横向
        // 缩放 1.0 倍时依然全覆盖裁剪框 (800x800)
        val (pan3, scale3) = ImageAdjustmentEngine.clampPanAndScale(
            pan = androidx.compose.ui.geometry.Offset(0f, 0f),
            scale = 1.0f,
            rotationDegrees = 90f,
            sourceWidth = 500f,
            sourceHeight = 1000f,
            cropBoxWidth = 800f,
            cropBoxHeight = 800f,
            constrainToImage = true
        )
        assertEquals(1.0f, scale3, 0.001f)
    }
}
