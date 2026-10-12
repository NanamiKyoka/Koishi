package com.nanami.koishi.feature.tools.prism_tank.engine

import kotlinx.serialization.Serializable

/**
 * 光棱坦克合成参数
 *
 * [innerThreshold] 为里图亮度压缩上限，[coverThreshold] 为表图亮度下限，
 * 两段区间不得重叠，否则里图会在正常亮度下显形。
 */
@Serializable
data class PrismTankParams(
    val innerThreshold: Int = DEFAULT_INNER_THRESHOLD,
    val coverThreshold: Int = DEFAULT_COVER_THRESHOLD,
    val innerContrast: Float = DEFAULT_CONTRAST,
    val coverContrast: Float = DEFAULT_CONTRAST,
    val isInnerGray: Boolean = false,
    val isCoverGray: Boolean = true,
    val isReverse: Boolean = false,
    val slope: Int = DEFAULT_SLOPE,
    val gap: Int = DEFAULT_GAP,
    val isRow: Boolean = true
) {
    val usableInnerThreshold: Int
        get() = innerThreshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)

    val usableCoverThreshold: Int
        get() = coverThreshold.coerceIn(usableInnerThreshold, MAX_THRESHOLD)

    companion object {
        const val DEFAULT_INNER_THRESHOLD = 24
        const val DEFAULT_COVER_THRESHOLD = 42
        const val DEFAULT_CONTRAST = 1f
        const val DEFAULT_SLOPE = 1
        const val DEFAULT_GAP = 1

        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 180
        const val MIN_CONTRAST = 0.5f
        const val MAX_CONTRAST = 1.5f
        const val MIN_SLOPE = 1
        const val MAX_SLOPE = 4
        const val MIN_GAP = 1
        const val MAX_GAP = 4
    }
}

/**
 * 还原时对非里图像素（即表图占位像素）的填充方式
 */
@Serializable
enum class PrismTankFill {
    BLACK,
    WHITE,
    TRANSPARENT,
    NEIGHBOR_AVG
}

/**
 * 光棱坦克还原参数
 *
 * 落在 [lowerThreshold, higherThreshold] 区间内的像素视为里图，线性拉伸回 0..255。
 */
@Serializable
data class PrismTankRevealConfig(
    val lowerThreshold: Int = 0,
    val higherThreshold: Int = PrismTankParams.DEFAULT_INNER_THRESHOLD,
    val contrast: Float = PrismTankParams.DEFAULT_CONTRAST,
    val fill: PrismTankFill = PrismTankFill.NEIGHBOR_AVG
) {
    val usableLower: Int
        get() = lowerThreshold.coerceIn(0, MAX_THRESHOLD)

    val usableUpper: Int
        get() = higherThreshold.coerceIn(usableLower + 1, MAX_THRESHOLD)

    companion object {
        const val MAX_THRESHOLD = 255
    }
}