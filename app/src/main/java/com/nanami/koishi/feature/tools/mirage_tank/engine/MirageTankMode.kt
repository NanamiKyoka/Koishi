package com.nanami.koishi.feature.tools.mirage_tank.engine

/**
 * 幻影坦克色彩模式
 */
enum class MirageTankMode {
    /**
     * 黑白模式（经典灰度 Alpha 解算，表图亮化 + 里图暗化）
     */
    GRAYSCALE,

    /**
     * 彩色模式（RGB 三通道加权解算，保留表图与里图真实色彩）
     */
    COLOR
}

/**
 * 幻影坦克生成参数配置
 *
 * @property mode 色彩模式（黑白/彩色）
 * @property frontLightness 表图亮度调节（0.5f ~ 1.5f，默认 1.0f）
 * @property frontContrast 表图对比度调节（0.5f ~ 1.5f，默认 1.0f）
 * @property backLightness 里图亮度调节（0.5f ~ 1.5f，默认 1.0f）
 * @property backContrast 里图对比度调节（0.5f ~ 1.5f，默认 1.0f）
 * @property enableCheckerboard 启用棋盘格调和（通过空间微交错平滑颜色与过渡边界）
 * @property checkerboardStrength 棋盘格调和强度（0.0f ~ 0.5f，默认 0.15f）
 */
data class MirageTankParams(
    val mode: MirageTankMode = MirageTankMode.GRAYSCALE,
    val frontLightness: Float = 1.0f,
    val frontContrast: Float = 1.0f,
    val backLightness: Float = 1.0f,
    val backContrast: Float = 1.0f,
    val enableCheckerboard: Boolean = false,
    val checkerboardStrength: Float = 0.15f
)
