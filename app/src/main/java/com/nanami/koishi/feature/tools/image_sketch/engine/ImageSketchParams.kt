package com.nanami.koishi.feature.tools.image_sketch.engine

/**
 * 素描参数配置
 *
 * @property radius 高斯模糊取样半径，用户可选 1..100，偶数将被自动调整为相邻奇数
 */
data class ImageSketchParams(
    val radius: Int = ImageSketchEngine.DEFAULT_RADIUS
) {
    /**
     * 实际投入运算的半径（区间收敛 + 奇数化）
     */
    val effectiveRadius: Int
        get() = ImageSketchEngine.normalizeRadius(radius)
}
