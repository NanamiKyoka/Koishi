package com.nanami.koishi.feature.tools.noise_meter.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp
import kotlin.math.log10

class NoiseProcessorTest {

    @Test
    fun `constant power produces steady readings`() {
        val processor = NoiseProcessor(TimeWeighting.FAST, 40.0)

        val frame = processor.process(0.5)

        val expected = 10.0 * log10(0.5)
        assertEquals(expected, frame.level, 1e-9)
        assertEquals(expected, frame.peak, 1e-9)
        assertEquals(expected, frame.minimum, 1e-9)
        assertEquals(expected, frame.leq, 1e-9)
        assertEquals(40L, frame.elapsedMs)
    }

    @Test
    fun `time weighting smooths towards the new power`() {
        val blockMs = 100.0
        val processor = NoiseProcessor(TimeWeighting.SLOW, blockMs)
        val alpha = 1.0 - exp(-blockMs / TimeWeighting.SLOW.timeConstantMs)

        processor.process(1.0)
        val frame = processor.process(0.01)

        val expectedPower = alpha * 0.01 + (1.0 - alpha) * 1.0
        assertEquals(10.0 * log10(expectedPower), frame.level, 1e-9)
        assertEquals(0.0, frame.peak, 1e-9)
        assertEquals(frame.level, frame.minimum, 1e-9)
    }

    @Test
    fun `leq averages power over the session`() {
        val processor = NoiseProcessor(TimeWeighting.FAST, 20.0)

        processor.process(1.0)
        val frame = processor.process(0.25)

        assertEquals(10.0 * log10(0.625), frame.leq, 1e-9)
        assertEquals(0.0, frame.peak, 1e-9)
        assertEquals(frame.level, frame.minimum, 1e-9)
        assertEquals(40L, frame.elapsedMs)
    }

    @Test
    fun `readings are floored for silence`() {
        val processor = NoiseProcessor(TimeWeighting.FAST, 20.0)

        val frame = processor.process(0.0)

        assertTrue(frame.level <= -100.0)
    }
}