package com.nanami.koishi.feature.tools.noise_meter.engine

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * IEC 61672 频率计权曲线，返回相对 1 kHz 的增益（dB）。
 */
internal object WeightingCurve {

    private const val F1 = 20.598997
    private const val F2 = 107.65265
    private const val F3 = 737.86223
    private const val F4 = 12194.217

    fun gainDb(weighting: FrequencyWeighting, frequencyHz: Double): Double {
        if (frequencyHz <= 0.0) return NEGATIVE_INFINITY
        return when (weighting) {
            FrequencyWeighting.Z -> 0.0
            FrequencyWeighting.A -> aWeightingDb(frequencyHz)
            FrequencyWeighting.C -> cWeightingDb(frequencyHz)
        }
    }

    private fun aWeightingDb(frequencyHz: Double): Double {
        val f2 = frequencyHz * frequencyHz
        val numerator = F4 * F4 * f2 * f2
        val denominator = (f2 + F1 * F1) *
            sqrt((f2 + F2 * F2) * (f2 + F3 * F3)) *
            (f2 + F4 * F4)
        return 20.0 * log10(numerator / denominator) + 2.0
    }

    private fun cWeightingDb(frequencyHz: Double): Double {
        val f2 = frequencyHz * frequencyHz
        val numerator = F4 * F4 * f2
        val denominator = (f2 + F1 * F1) * (f2 + F4 * F4)
        return 20.0 * log10(numerator / denominator) + 0.06
    }

    private const val NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY
}