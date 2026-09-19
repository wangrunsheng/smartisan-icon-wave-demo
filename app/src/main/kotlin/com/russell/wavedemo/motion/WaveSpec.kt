package com.russell.wavedemo.motion

import kotlin.math.PI
import kotlin.math.sin

/** One traveling wave, expressed in the reference recording's 180-unit coordinate space. */
data class WaveSpec(
    val amplitude: Float,
    val periodSeconds: Double,
    val phaseRadians: Double,
    val opacity: Int,
    val levelOffset: Float = 0f,
) {
    init {
        require(amplitude >= 0f && amplitude.isFinite())
        require(periodSeconds > 0.0 && periodSeconds.isFinite())
        require(phaseRadians.isFinite())
        require(opacity in 0..255)
        require(levelOffset.isFinite())
    }

    /**
     * Vertical displacement in Canvas coordinates (positive points down).
     * Increasing time moves the wave left. The phase matches the previous Bézier
     * version at its crests and troughs, so only the shape between extrema changes.
     */
    fun offsetAt(x: Float, seconds: Double): Float {
        val phase = 2 * PI * (x / WAVELENGTH + (seconds % periodSeconds) / periodSeconds) +
            phaseRadians
        return levelOffset + amplitude * sin(phase).toFloat()
    }

    /** Wrap after one wavelength to keep translation bounded without a visible seam. */
    fun translationAt(seconds: Double): Float {
        val cycle = ((seconds % periodSeconds) / periodSeconds + 1.0) % 1.0
        return (-WAVELENGTH * cycle).toFloat()
    }

    companion object {
        const val WAVELENGTH = 180f

        val Front = WaveSpec(3.72f, 0.8493, -0.288916, 255)
        val Rear = WaveSpec(5.74f, 0.6993, 0.774, 77, -0.6454f)
    }
}
