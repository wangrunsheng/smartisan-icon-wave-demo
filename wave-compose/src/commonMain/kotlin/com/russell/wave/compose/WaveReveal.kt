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
import com.russell.wave.core.WaveRevealDefaults
import com.russell.wave.core.WaveRevealSpec
import com.russell.wave.core.WaveSamples

/**
 * Reveals one live content tree using an automatically managed wave clock.
 * [progress] controls water level, not elapsed time, and is clamped to 0..1.
 * The clock pauses at either endpoint, when [running] is false, or [speed] is zero.
 * Set running=false when the host is inactive or a retained item is offscreen.
 * [shape] clips all slots; backdrop and overlay are outside the wave mask.
 */
@Composable
fun WaveReveal(
    progress: Float,
    modifier: Modifier = Modifier,
    spec: WaveRevealSpec = WaveRevealDefaults.DoubleWave,
    running: Boolean = true,
    speed: Double = 1.0,
    shape: Shape = RectangleShape,
    backdrop: @Composable BoxScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val time = rememberAutomaticWaveTime(progress, running, speed)
    WaveReveal(progress, { time.value }, modifier, spec, shape, backdrop, overlay, content)
}

/**
 * Advanced overload for a shared or externally driven clock. No internal clock is started.
 * [timeSeconds] is read only during drawing; elapsed time does not recompose content.
 * Give the container a size, or let content determine its natural size.
 * Visual masking does not change input or accessibility semantics.
 */
@Composable
fun WaveReveal(
    progress: Float,
    timeSeconds: () -> Double,
    modifier: Modifier = Modifier,
    spec: WaveRevealSpec = WaveRevealDefaults.DoubleWave,
    shape: Shape = RectangleShape,
    backdrop: @Composable BoxScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.clip(shape), propagateMinConstraints = true) {
        Box(Modifier.matchParentSize(), content = backdrop)
        Box(
            Modifier.waveReveal(progress, timeSeconds, spec),
            propagateMinConstraints = true,
            content = content,
        )
        Box(Modifier.matchParentSize(), content = overlay)
    }
}

/**
 * Applies the reveal directly to existing content without adding a layout container.
 * Modifier order matters: background().waveReveal() keeps the background outside
 * the mask; waveReveal().background() masks it. Use clip(shape) for outer clipping.
 * Input and semantics are unchanged. [running] controls this modifier's own clock.
 * Call from a composable; retained offscreen items should pass running=false.
 */
@Composable
fun Modifier.waveReveal(
    progress: Float,
    spec: WaveRevealSpec = WaveRevealDefaults.DoubleWave,
    running: Boolean = true,
    speed: Double = 1.0,
): Modifier {
    val time = rememberAutomaticWaveTime(progress, running, speed)
    return waveReveal(progress, { time.value }, spec)
}

/** External-clock variant; the caller owns pause, speed and lifecycle policy. */
@Composable
fun Modifier.waveReveal(
    progress: Float,
    timeSeconds: () -> Double,
    spec: WaveRevealSpec = WaveRevealDefaults.DoubleWave,
): Modifier {
    require(progress.isFinite()) { "progress must be finite; values outside 0..1 are clamped" }
    val currentProgress = rememberUpdatedState(progress.coerceIn(0f, 1f))
    val currentTime = rememberUpdatedState(timeSeconds)
    // Remember the modifier so equal configuration values retain cached geometry
    // when the caller recomposes for unrelated reasons or changes progress.
    return this.then(remember(spec) {
        Modifier.waveMask(spec, { currentProgress.value }, { currentTime.value() })
    })
}

@Composable
private fun rememberAutomaticWaveTime(progress: Float, running: Boolean, speed: Double): State<Double> {
    require(progress.isFinite()) { "progress must be finite; values outside 0..1 are clamped" }
    return rememberWaveTime(running && progress > 0f && progress < 1f, speed)
}

/**
 * Frame-clock time that preserves its position across pause/resume.
 * Set running=false at endpoints, offscreen or when the host is inactive.
 * Leaving composition cancels the loop. Share this State to synchronize cards.
 */
@Composable
fun rememberWaveTime(running: Boolean = true, speed: Double = 1.0): State<Double> {
    require(speed.isFinite() && speed >= 0.0) { "speed must be finite and non-negative" }
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
            require(seconds.isFinite()) { "timeSeconds must return a finite value" }
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
