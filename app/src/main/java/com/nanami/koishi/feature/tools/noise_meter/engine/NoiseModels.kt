package com.nanami.koishi.feature.tools.noise_meter.engine

import kotlinx.serialization.Serializable

enum class FrequencyWeighting {
    A,
    C,
    Z
}

enum class TimeWeighting(val timeConstantMs: Double) {
    FAST(125.0),
    SLOW(1000.0)
}

enum class NoiseLevel(val upperBoundDb: Double) {
    VERY_QUIET(30.0),
    QUIET(40.0),
    MODERATE(55.0),
    NOISY(70.0),
    VERY_NOISY(85.0),
    DANGEROUS(100.0),
    EXTREME(Double.MAX_VALUE);

    companion object {
        fun fromDb(db: Double): NoiseLevel = entries.first { db < it.upperBoundDb }
    }
}

data class NoiseConfig(
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.A,
    val timeWeighting: TimeWeighting = TimeWeighting.FAST,
    val calibrationOffsetDb: Double = DEFAULT_CALIBRATION_OFFSET_DB
) {
    companion object {
        const val DEFAULT_CALIBRATION_OFFSET_DB = 100.0
        const val MIN_CALIBRATION_OFFSET_DB = 60.0
        const val MAX_CALIBRATION_OFFSET_DB = 130.0
    }
}

/**
 * 单帧读数，除 [elapsedMs] 外的分贝值均为未加校准偏移的 dBFS，由上层叠加偏移后展示。
 */
data class NoiseFrame(
    val level: Double,
    val peak: Double,
    val minimum: Double,
    val leq: Double,
    val elapsedMs: Long
) {
    companion object {
        val Empty = NoiseFrame(0.0, 0.0, 0.0, 0.0, 0L)
    }
}

@Serializable
data class NoiseMeterData(
    val calibrationOffsetDb: Double = NoiseConfig.DEFAULT_CALIBRATION_OFFSET_DB,
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.A,
    val timeWeighting: TimeWeighting = TimeWeighting.FAST
)