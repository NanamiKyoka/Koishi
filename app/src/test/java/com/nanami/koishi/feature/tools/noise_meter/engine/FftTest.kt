package com.nanami.koishi.feature.tools.noise_meter.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

class FftTest {

    @Test
    fun `constant signal converges into dc bin`() {
        val size = 64
        val real = DoubleArray(size) { 1.0 }
        val imag = DoubleArray(size)

        Fft.transform(real, imag)

        assertEquals(size.toDouble(), real[0], 1e-9)
        assertEquals(0.0, imag[0], 1e-9)
        for (bin in 1 until size) {
            assertEquals(0.0, hypot(real[bin], imag[bin]), 1e-9)
        }
    }

    @Test
    fun `sine wave concentrates energy in a single bin`() {
        val size = 64
        val targetBin = 5
        val real = DoubleArray(size) { sin(2.0 * PI * targetBin * it / size) }
        val imag = DoubleArray(size)

        Fft.transform(real, imag)

        assertEquals(size / 2.0, hypot(real[targetBin], imag[targetBin]), 1e-9)
        assertEquals(0.0, hypot(real[0], imag[0]), 1e-9)
    }
}