package com.russell.wavedemo.motion

import kotlin.math.PI

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

    /** Negative displacement moves the fixed curve left, without changing its control points. */
    fun originAt(seconds: Double): Float {
        val shift = WAVELENGTH / periodSeconds * seconds +
            (phaseRadians + PI / 2) * WAVELENGTH / (2 * PI)
        return (-(shift % WAVELENGTH) - WAVELENGTH).toFloat()
    }

    companion object {
        const val WAVELENGTH = 180f
        const val HANDLE_RATIO = 0.36f

        val Front = WaveSpec(3.72f, 0.8493, -0.288916, 255)
        val Rear = WaveSpec(5.74f, 0.6993, 0.774, 77, -0.6454f)
    }
}
