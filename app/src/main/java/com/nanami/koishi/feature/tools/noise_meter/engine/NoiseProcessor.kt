package com.nanami.koishi.feature.tools.noise_meter.engine

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * 对每个音频块的均方值做时间计权并累计会话统计，输出单位为 dBFS 的原始帧。
 */
internal class NoiseProcessor(
    timeWeighting: TimeWeighting,
    private val blockDurationMs: Double,
    private val floorDb: Double = MIN_DB
) {

    private val alpha = (1.0 - exp(-blockDurationMs / timeWeighting.timeConstantMs))
        .coerceIn(0.0, 1.0)

    private var timeWeightedPower = 0.0
    private var initialized = false
    private var powerSum = 0.0
    private var blockCount = 0L
    private var peakDb = floorDb
    private var minimumDb = Double.MAX_VALUE
    private var elapsedMs = 0.0

    fun process(meanSquare: Double): NoiseFrame {
        val power = meanSquare.coerceAtLeast(EPSILON)
        timeWeightedPower = if (initialized) {
            alpha * power + (1.0 - alpha) * timeWeightedPower
        } else {
            initialized = true
            power
        }
        powerSum += power
        blockCount++
        elapsedMs += blockDurationMs

        val level = toDb(timeWeightedPower)
        val instantaneous = toDb(power)
        val leq = toDb(powerSum / blockCount)
        peakDb = max(peakDb, instantaneous)
        minimumDb = min(minimumDb, level)

        return NoiseFrame(
            level = level,
            peak = peakDb,
            minimum = minimumDb,
            leq = leq,
            elapsedMs = elapsedMs.toLong()
        )
    }

    private fun toDb(power: Double): Double =
        (10.0 * log10(power)).coerceAtLeast(floorDb)

    companion object {
        private const val MIN_DB = -120.0
        private const val EPSILON = 1e-12
    }
}