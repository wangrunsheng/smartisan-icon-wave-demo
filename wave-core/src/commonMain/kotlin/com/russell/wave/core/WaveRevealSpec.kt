package com.russell.wave.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Horizontal travel direction; vertical progress is independent of travel. */
enum class WaveDirection { Left, Right }

/**
 * One sine mask. Lengths are relative to the viewport, not fixed device pixels.
 * [amplitudeFraction] and [levelOffsetFraction] use the shorter side;
 * [wavelengthFraction] uses the width. Positive offsets point downwards.
 */
data class WaveLayerSpec(
    val amplitudeFraction: Float = 0.021f,
    val wavelengthFraction: Float = 1f,
    val periodSeconds: Double = 0.85,
    val phaseRadians: Double = 0.0,
    val opacity: Float = 1f,
    val levelOffsetFraction: Float = 0f,
    val direction: WaveDirection = WaveDirection.Left,
) {
    init {
        require(amplitudeFraction.isFinite() && amplitudeFraction in 0f..1f)
        require(wavelengthFraction.isFinite() && wavelengthFraction in 0.1f..10f)
        require(periodSeconds.isFinite() && periodSeconds > 0.0)
        require(phaseRadians.isFinite())
        require(opacity.isFinite() && opacity in 0f..1f)
        require(levelOffsetFraction.isFinite() && levelOffsetFraction in -1f..1f)
    }

    /** Always in [-wavelength, 0], including reverse motion and negative time. */
    fun translationX(width: Float, seconds: Double): Float {
        require(width.isFinite() && width > 0f && seconds.isFinite())
        val directionSign = if (direction == WaveDirection.Left) 1 else -1
        val cycle = ((seconds % periodSeconds) / periodSeconds * directionSign + 1.0) % 1.0
        return (-width * wavelengthFraction * cycle).toFloat()
    }

    fun offsetAt(x: Float, width: Float, height: Float, seconds: Double = 0.0): Float {
        require(width.isFinite() && width > 0f && height.isFinite() && height > 0f)
        require(x.isFinite() && seconds.isFinite())
        val shortSide = min(width, height)
        val directionSign = if (direction == WaveDirection.Left) 1 else -1
        val phase = 2 * PI * (x.toDouble() / (width * wavelengthFraction) +
            directionSign * (seconds % periodSeconds) / periodSeconds) + phaseRadians
        return shortSide * (levelOffsetFraction + amplitudeFraction * sin(phase).toFloat())
    }
}

/**
 * A single content tree revealed by one or more independently moving masks.
 * Coverage uses source-over: M = 1 - (1-baseOpacity) * product(1-opacity_i*mask_i).
 * This mask multiplies the content's own alpha; it never makes transparent pixels opaque.
 */
class WaveRevealSpec(
    waves: List<WaveLayerSpec> = listOf(
        WaveLayerSpec(0.032f, periodSeconds = 0.70, phaseRadians = 0.77, opacity = 0.30f),
        WaveLayerSpec(0.021f, periodSeconds = 0.85, phaseRadians = -0.29),
    ),
    val baseOpacity: Float = 0f,
    val contentOpacity: Float = 1f,
) {
    // Copy to prevent a caller's mutable list from invalidating cached geometry silently.
    val waves: List<WaveLayerSpec> = waves.toList()

    init {
        require(this.waves.isNotEmpty() && this.waves.size <= 8)
        require(baseOpacity.isFinite() && baseOpacity in 0f..1f)
        require(contentOpacity.isFinite() && contentOpacity in 0f..1f)
    }

    fun margin(width: Float, height: Float): Float =
        min(width, height) * waves.maxOf { it.amplitudeFraction + abs(it.levelOffsetFraction) } + 1f

    /** 0 hides all wave masks; 1 covers the viewport with every mask, including extrema. */
    fun baseline(progress: Float, width: Float, height: Float): Float {
        require(progress.isFinite())
        val p = progress.coerceIn(0f, 1f)
        val margin = margin(width, height)
        return (height + margin) * (1 - p) - margin * p
    }

    /** Reference coverage calculation, useful for tests and other rendering backends. */
    fun opacityAt(x: Float, y: Float, width: Float, height: Float, progress: Float, seconds: Double): Float {
        val level = baseline(progress, width, height)
        var remaining = 1f - baseOpacity
        for (wave in waves) {
            if (y >= level + wave.offsetAt(x, width, height, seconds)) remaining *= 1f - wave.opacity
        }
        return (1f - remaining) * contentOpacity
    }
}

/** Platform-neutral sampled geometry. Generate on size/spec changes, never every frame. */
class WaveSamples(wave: WaveLayerSpec, width: Float, height: Float, maxErrorPx: Float = 0.15f) {
    val coordinates: FloatArray
    val endX: Float

    init {
        require(width.isFinite() && width > 0 && height.isFinite() && height > 0)
        require(maxErrorPx.isFinite() && maxErrorPx > 0)
        val wavelength = width * wave.wavelengthFraction
        val amplitude = min(width, height) * wave.amplitudeFraction
        // Linear interpolation error <= A * pi^2 / (2*n^2) for n samples per period.
        val segments = max(16, ceil(PI * sqrt(amplitude / (2.0 * maxErrorPx))).toInt())
        val periods = ceil(width / wavelength).toInt() + 1
        val count = segments * periods
        endX = wavelength * periods
        coordinates = FloatArray((count + 1) * 2)
        for (index in 0..count) {
            val x = wavelength * (index.toFloat() / segments)
            coordinates[index * 2] = x
            coordinates[index * 2 + 1] = wave.offsetAt(x, width, height)
        }
    }
}
