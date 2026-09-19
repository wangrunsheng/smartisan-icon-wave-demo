package com.russell.wavedemo.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun `resuming does not count time spent in the background`() {
        val clock = PlaybackClock()
        clock.advance(1_000_000_000L, 1.0)
        clock.advance(1_500_000_000L, 1.0)
        clock.resetFrameAnchor()
        clock.advance(100_000_000_000L, 1.0)
        assertEquals(8.5, clock.seconds, 0.000001)
        clock.advance(100_100_000_000L, 1.0)
        assertEquals(8.6, clock.seconds, 0.000001)
    }

    @Test
    fun `slow playback advances one quarter as far`() {
        val clock = PlaybackClock()
        clock.advance(0L, .25)
        clock.advance(1_000_000_000L, .25)
        assertEquals(8.25, clock.seconds, 0.000001)
    }

    @Test
    fun `replay preserves elapsed time beyond its boundary`() {
        val clock = PlaybackClock(11.9)
        clock.advance(0L, 1.0)
        clock.advance(300_000_000L, 1.0)
        assertEquals(8.2, clock.seconds, 0.000001)
    }

    @Test
    fun `vertical progress stays bounded and never falls within the excerpt`() {
        var previous = WaterLevel.at(8.0)
        for (step in 0..400) {
            val current = WaterLevel.at(8.0 + step / 100.0)
            assertTrue(current <= previous + 0.0001f)
            assertTrue(current in 59.55f..76.49f)
            previous = current
        }
        assertEquals(76.48f, WaterLevel.at(-1.0), 0.0001f)
        assertEquals(59.56f, WaterLevel.at(20.0), 0.0001f)
    }

    @Test
    fun `each wave returns to its phase after its own period`() {
        for (wave in listOf(WaveSpec.Front, WaveSpec.Rear)) {
            for (x in listOf(0f, 45f, 92f, 180f)) {
                assertEquals(wave.offsetAt(x, 8.2), wave.offsetAt(x, 8.2 + wave.periodSeconds), 0.0001f)
                assertEquals(wave.offsetAt(x, 8.2), wave.offsetAt(x + WaveSpec.WAVELENGTH, 8.2), 0.0001f)
                // A quarter-period of time equals a quarter-wavelength to the right:
                // the visible curve therefore travels to the left.
                assertEquals(
                    wave.offsetAt(x, 8.2 + wave.periodSeconds / 4),
                    wave.offsetAt(x + WaveSpec.WAVELENGTH / 4, 8.2),
                    0.0001f,
                )
            }
        }
        assertTrue(WaveSpec.Rear.periodSeconds < WaveSpec.Front.periodSeconds)
    }

    @Test
    fun `sine reaches the configured extrema and crosses its center`() {
        val wave = WaveSpec(6f, 1.0, 0.0, 255, 2f)
        assertEquals(2f, wave.offsetAt(0f, 0.0), 0.0001f)
        assertEquals(8f, wave.offsetAt(45f, 0.0), 0.0001f)
        assertEquals(2f, wave.offsetAt(90f, 0.0), 0.0001f)
        assertEquals(-4f, wave.offsetAt(135f, 0.0), 0.0001f)
    }
    @Test
    fun `cached translation matches analytic wave across wrap boundaries`() {
        for (wave in listOf(WaveSpec.Front, WaveSpec.Rear)) {
            for (cycles in listOf(-0.01, 0.0, 0.999999, 1.0, 1.000001, 100.25)) {
                val time = cycles * wave.periodSeconds
                val dx = wave.translationAt(time)
                assertTrue(dx >= -WaveSpec.WAVELENGTH && dx <= 0f)
                for (x in listOf(0f, 45f, 92f, 180f)) {
                    assertEquals(wave.offsetAt(x, time), wave.offsetAt(x - dx, 0.0), 0.0001f)
                }
            }
        }
    }
}
