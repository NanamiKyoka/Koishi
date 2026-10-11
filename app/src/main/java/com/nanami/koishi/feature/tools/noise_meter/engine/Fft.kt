package com.nanami.koishi.feature.tools.noise_meter.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object Fft {

    fun transform(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        require(n == imag.size) { "real and imaginary buffers must match" }
        require(n > 0 && n and (n - 1) == 0) { "size must be a power of two" }
        if (n == 1) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = real[i]
                real[i] = real[j]
                real[j] = tr
                val ti = imag[i]
                imag[i] = imag[j]
                imag[j] = ti
            }
        }

        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val stepReal = cos(angle)
            val stepImag = sin(angle)
            val half = length shr 1
            var start = 0
            while (start < n) {
                var twiddleReal = 1.0
                var twiddleImag = 0.0
                for (k in 0 until half) {
                    val evenIndex = start + k
                    val oddIndex = evenIndex + half
                    val oddReal = real[oddIndex] * twiddleReal - imag[oddIndex] * twiddleImag
                    val oddImag = real[oddIndex] * twiddleImag + imag[oddIndex] * twiddleReal
                    real[oddIndex] = real[evenIndex] - oddReal
                    imag[oddIndex] = imag[evenIndex] - oddImag
                    real[evenIndex] += oddReal
                    imag[evenIndex] += oddImag
                    val nextReal = twiddleReal * stepReal - twiddleImag * stepImag
                    twiddleImag = twiddleReal * stepImag + twiddleImag * stepReal
                    twiddleReal = nextReal
                }
                start += length
            }
            length = length shl 1
        }
    }
}