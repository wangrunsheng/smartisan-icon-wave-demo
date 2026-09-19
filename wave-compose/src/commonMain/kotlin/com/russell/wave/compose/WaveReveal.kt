package com.russell.wave.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.russell.wave.core.WaveRevealSpec
import com.russell.wave.core.WaveSamples

/**
 * One live content tree, revealed through a union of sine masks.
 * Backdrop and overlay are not masked. [shape] clips all three slots.
 * Give this container a size, or let content determine its size.
 * Masking is visual only: callers own input, semantics and platform-native surfaces.
 * [timeSeconds] is read during drawing, so animation does not recompose content.
 */
@Composable
fun WaveReveal(
    progress: Float,
    timeSeconds: () -> Double,
    modifier: Modifier = Modifier,
    spec: WaveRevealSpec = remember { WaveRevealSpec() },
    shape: Shape = RectangleShape,
    backdrop: @Composable BoxScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    require(progress.isFinite())
    val currentProgress = rememberUpdatedState(progress.coerceIn(0f, 1f))
    val currentTime = rememberUpdatedState(timeSeconds)
    Box(modifier.clip(shape), propagateMinConstraints = true) {
        Box(Modifier.matchParentSize(), content = backdrop)
        Box(
            Modifier.waveMask(spec, { currentProgress.value }, { currentTime.value() }),
            propagateMinConstraints = true,
            content = content,
        )
        Box(Modifier.matchParentSize(), content = overlay)
    }
}

/**
 * Frame-clock time that preserves its position across pause/resume.
 * Set running=false at endpoints, offscreen or when the host is inactive.
 * Leaving composition cancels the loop. Share this State to synchronize cards.
 */
@Composable
fun rememberWaveTime(running: Boolean = true, speed: Double = 1.0): State<Double> {
    require(speed.isFinite() && speed >= 0.0)
    val elapsed = remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(running, speed) {
        if (running && speed > 0.0) {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                elapsed.doubleValue += (now - previous) / 1e9 * speed
                previous = now
            }
        }
    }
    return elapsed
}

private fun Modifier.waveMask(
    spec: WaveRevealSpec,
    progress: () -> Float,
    time: () -> Double,
) = drawWithCache {
    val bounds = Rect(0f, 0f, size.width, size.height)
    val paths = if (size.width > 0 && size.height > 0) spec.waves.map { wave ->
        val samples = WaveSamples(wave, size.width, size.height)
        Path().apply {
            moveTo(samples.coordinates[0], samples.coordinates[1])
            for (i in 2 until samples.coordinates.size step 2) {
                lineTo(samples.coordinates[i], samples.coordinates[i + 1])
            }
            val bottom = size.height + 2 * spec.margin(size.width, size.height)
            lineTo(samples.endX, bottom)
            lineTo(0f, bottom)
            close()
        }
    } else emptyList()
    val group = Paint().apply { alpha = spec.contentOpacity }
    val mask = Paint().apply { blendMode = BlendMode.DstIn }
    val clear = Paint().apply { color = Color.Transparent; blendMode = BlendMode.Src }
    val fill = Paint().apply { color = Color.Black }
    onDrawWithContent {
        if (paths.isNotEmpty()) {
            val seconds = time()
            require(seconds.isFinite())
            val baseline = spec.baseline(progress(), size.width, size.height)
            drawIntoCanvas { canvas ->
                canvas.saveLayer(bounds, group)
                drawContent()
                canvas.saveLayer(bounds, mask)
                // Explicit transparent coverage also handles older Android HWUI backends.
                canvas.drawRect(bounds, clear)
                fill.alpha = spec.baseOpacity
                canvas.drawRect(bounds, fill)
                spec.waves.forEachIndexed { index, wave ->
                    canvas.save()
                    canvas.translate(wave.translationX(size.width, seconds), baseline)
                    fill.alpha = wave.opacity
                    canvas.drawPath(paths[index], fill)
                    canvas.restore()
                }
                canvas.restore()
                canvas.restore()
            }
        }
    }
}
