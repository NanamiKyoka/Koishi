package com.nanami.koishi.feature.tools.noise_meter.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * 对单块归一化采样做加窗 FFT，按频率计权曲线累计均方值，单位相对满量程。
 */
internal class WeightedPowerAnalyzer(
    weighting: FrequencyWeighting,
    private val sampleRate: Int,
    private val blockSize: Int
) {

    private val window = DoubleArray(blockSize) { index ->
        0.5 * (1.0 - cos(2.0 * PI * index / (blockSize - 1)))
    }

    private val windowMeanSquare = window.sumOf { it * it } / blockSize

    private val powerGains = DoubleArray(blockSize / 2 + 1) { bin ->
        10.0.pow(WeightingCurve.gainDb(weighting, bin.toDouble() * sampleRate / blockSize) / 10.0)
    }

    private val real = DoubleArray(blockSize)
    private val imag = DoubleArray(blockSize)

    fun analyze(samples: DoubleArray): Double {
        for (i in 0 until blockSize) {
            real[i] = samples[i] * window[i]
            imag[i] = 0.0
        }
        Fft.transform(real, imag)

        var sum = 0.0
        for (bin in 0..blockSize / 2) {
            val magnitude = real[bin] * real[bin] + imag[bin] * imag[bin]
            val folded = if (bin == 0 || bin == blockSize / 2) magnitude else 2.0 * magnitude
            sum += folded * powerGains[bin]
        }
        return sum / (blockSize.toDouble() * blockSize * windowMeanSquare)
    }
}