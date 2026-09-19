package com.russell.wave.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WaveRevealTest {
    @Test fun endpointsCoverArbitraryAspectRatiosAndOffsets() {
        val spec = WaveRevealSpec(listOf(
            WaveLayerSpec(amplitudeFraction = .4f, levelOffsetFraction = -.6f, opacity = .3f),
            WaveLayerSpec(amplitudeFraction = .2f, levelOffsetFraction = .7f),
        ))
        for ((w, h) in listOf(80f to 600f, 900f to 40f, 180f to 180f)) {
            for (t in listOf(0.0, .23, 10.99)) for (x in listOf(0f, w / 2, w)) {
                assertEquals(0f, spec.opacityAt(x, h, w, h, 0f, t), .0001f)
                assertEquals(1f, spec.opacityAt(x, 0f, w, h, 1f, t), .0001f)
            }
        }
    }

    @Test fun masksUseSourceOverAndRespectGroupOpacity() {
        val wave = WaveLayerSpec(opacity = .3f)
        val spec = WaveRevealSpec(listOf(wave, wave), contentOpacity = .5f)
        assertEquals(.255f, spec.opacityAt(50f, 50f, 100f, 100f, 1f, 0.0), .0001f)
        val base = WaveRevealSpec(listOf(wave), baseOpacity = .2f)
        assertEquals(.2f, base.opacityAt(50f, 50f, 100f, 100f, 0f, 0.0), .0001f)
    }

    @Test fun cachedTranslationEqualsTheAnalyticCurveInBothDirections() {
        for (direction in WaveDirection.entries) {
            val wave = WaveLayerSpec(wavelengthFraction = .37f, direction = direction)
            for (time in listOf(-.001, 0.0, .849999, .85, .850001, 1000.0)) {
                val dx = wave.translationX(480f, time)
                assertTrue(dx in -177.6f..0f)
                for (x in listOf(0f, 123f, 480f)) {
                    assertEquals(wave.offsetAt(x, 480f, 240f, time),
                        wave.offsetAt(x - dx, 480f, 240f), .001f)
                }
            }
        }
    }

    @Test fun samplesCoverTheViewportAndMeetErrorBound() {
        for (ratio in listOf(.1f, 1f, 10f)) {
            val wave = WaveLayerSpec(amplitudeFraction = .3f, wavelengthFraction = ratio)
            val samples = WaveSamples(wave, 300f, 120f)
            assertTrue(samples.endX >= 300f + 300f * ratio - .001f)
            val points = samples.coordinates
            for (i in 0 until points.size - 2 step 2) {
                for (fraction in listOf(.25f, .5f, .75f)) {
                    val x = points[i] + fraction * (points[i + 2] - points[i])
                    val y = points[i + 1] + fraction * (points[i + 3] - points[i + 1])
                    assertTrue(abs(y - wave.offsetAt(x, 300f, 120f)) <= .151f)
                }
            }
        }
    }

    @Test fun invalidParametersFailEarlyAndInputListsAreCopied() {
        assertFailsWith<IllegalArgumentException> { WaveLayerSpec(periodSeconds = 0.0) }
        assertFailsWith<IllegalArgumentException> { WaveLayerSpec(opacity = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { WaveLayerSpec(wavelengthFraction = 0f) }
        assertFailsWith<IllegalArgumentException> { WaveRevealSpec(emptyList()) }
        val mutable = mutableListOf(WaveLayerSpec())
        val spec = WaveRevealSpec(mutable)
        mutable.clear()
        assertEquals(1, spec.waves.size)
    }
}
