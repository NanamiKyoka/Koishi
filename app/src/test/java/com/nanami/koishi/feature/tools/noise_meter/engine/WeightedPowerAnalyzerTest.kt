package com.nanami.koishi.feature.tools.noise_meter.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

class WeightedPowerAnalyzerTest {

    private val sampleRate = 48000
    private val blockSize = 2048

    private fun sine(bin: Int, amplitude: Double = 1.0): DoubleArray =
        DoubleArray(blockSize) { amplitude * sin(2.0 * PI * bin * it / blockSize) }

    private fun analyzer(weighting: FrequencyWeighting) =
        WeightedPowerAnalyzer(weighting, sampleRate, blockSize)

    @Test
    fun `flat weighting measures a full scale sine near minus three dbfs`() {
        val meanSquare = analyzer(FrequencyWeighting.Z).analyze(sine(bin = 64))

        assertEquals(0.5, meanSquare, 0.01)
        assertEquals(-3.01, 10.0 * log10(meanSquare), 0.05)
    }

    @Test
    fun `a weighting attenuates a low frequency tone`() {
        val signal = sine(bin = 4)
        val flat = analyzer(FrequencyWeighting.Z).analyze(signal)
        val weighted = analyzer(FrequencyWeighting.A).analyze(signal)

        val measuredAttenuation = 10.0 * log10(weighted / flat)
        val expectedAttenuation = WeightingCurve.gainDb(FrequencyWeighting.A, 4.0 * sampleRate / blockSize)
        assertEquals(expectedAttenuation, measuredAttenuation, 2.0)
    }

    @Test
    fun `a weighting keeps a one kilohertz tone nearly unchanged`() {
        val bin = 43
        val signal = sine(bin)
        val flat = analyzer(FrequencyWeighting.Z).analyze(signal)
        val weighted = analyzer(FrequencyWeighting.A).analyze(signal)

        assertEquals(0.0, 10.0 * log10(weighted / flat), 0.3)
    }
}