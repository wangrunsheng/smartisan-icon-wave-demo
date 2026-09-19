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
            assertEquals(wave.originAt(8.2), wave.originAt(8.2 + wave.periodSeconds), 0.0001f)
        }
        assertTrue(WaveSpec.Rear.periodSeconds < WaveSpec.Front.periodSeconds)
    }
}
