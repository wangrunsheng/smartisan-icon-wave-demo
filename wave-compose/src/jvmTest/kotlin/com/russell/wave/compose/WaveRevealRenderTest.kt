package com.russell.wave.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.russell.wave.core.WaveLayerSpec
import com.russell.wave.core.WaveRevealSpec
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the actual shared renderer, including offscreen alpha composition. */
class WaveRevealRenderTest {
    @Test
    fun renderedCoverageMatchesFormulaAndPreservesContentAlpha() {
        val spec = WaveRevealSpec(
            listOf(WaveLayerSpec(opacity = .3f), WaveLayerSpec(opacity = .3f)),
            baseOpacity = .2f,
            contentOpacity = .5f,
        )
        for (progress in listOf(0f, .5f, 1f)) {
            val scene = ImageComposeScene(100, 100) {
                WaveReveal(progress, { .2 }, Modifier.size(100.dp), spec = spec) {
                    Canvas(Modifier.size(100.dp)) {
                        // Leave half the content transparent to catch accidental opaque fills.
                        drawRect(Color.Blue.copy(alpha = .5f), size = Size(50f, 100f))
                    }
                }
            }
            try {
                val image = scene.render(0)
                val pixels = image.encodeToData()!!.use { data ->
                    ImageIO.read(ByteArrayInputStream(data.bytes))
                }
                image.close()
                for (y in listOf(10, 90)) {
                    val expected = spec.opacityAt(25.5f, y + .5f, 100f, 100f, progress, .2) * .5f
                    val actual = (pixels.getRGB(25, y) ushr 24) / 255f
                    assertTrue(abs(actual - expected) < .015f, "$progress: expected $expected, got $actual")
                    assertEquals(0, pixels.getRGB(75, y) ushr 24)
                }
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun fixedOverlayAndRoundedClipSurviveHiddenContent() {
        val scene = ImageComposeScene(100, 100) {
            WaveReveal(0f, { 0.0 }, Modifier.size(100.dp), shape = RoundedCornerShape(20.dp),
                backdrop = { Canvas(Modifier.size(100.dp)) { drawRect(Color.Green) } },
                overlay = {
                    Canvas(Modifier.size(100.dp)) {
                        drawRect(Color.Red, Offset(40f, 40f), Size(20f, 20f))
                    }
                },
            ) {
                Canvas(Modifier.size(100.dp)) { drawRect(Color.Blue) }
            }
        }
        try {
            val image = scene.render(0)
            val pixels = image.encodeToData()!!.use { ImageIO.read(ByteArrayInputStream(it.bytes)) }
            image.close()
            assertEquals(0xFFFF0000.toInt(), pixels.getRGB(50, 50))
            assertEquals(0, pixels.getRGB(0, 0) ushr 24)
            assertEquals(0xFF00FF00.toInt(), pixels.getRGB(20, 50))
        } finally {
            scene.close()
        }
    }

    @Test
    fun changingTimeRedrawsWithoutRecomposingContent() {
        val time = mutableDoubleStateOf(0.0)
        var compositions = 0
        val scene = ImageComposeScene(100, 100) {
            WaveReveal(.5f, { time.doubleValue }, Modifier.size(100.dp)) {
                compositions++
                Canvas(Modifier.size(100.dp)) { drawRect(Color.Blue) }
            }
        }
        try {
            val firstImage = scene.render(0)
            val first = firstImage.encodeToData()!!.use { it.bytes }
            firstImage.close()
            val initial = compositions
            time.doubleValue = .3
            val nextImage = scene.render(16_000_000)
            val next = nextImage.encodeToData()!!.use { it.bytes }
            nextImage.close()
            assertTrue(!first.contentEquals(next), "Time must update rendered pixels")
            assertEquals(1, initial)
            assertEquals(initial, compositions)
        } finally {
            scene.close()
        }
    }
}
