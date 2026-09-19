package com.russell.wavedemo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.russell.wave.core.WaveLayerSpec
import com.russell.wave.core.WaveSamples
import kotlin.math.PI

/** Same clock, amplitudes, phases and opacity; only the curve construction differs. */
class CurveComparisonView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bezier = Path()
    private val waves = listOf(
        WaveLayerSpec(5.74f / 180f, periodSeconds = .6993, phaseRadians = .774, opacity = 77f / 255, levelOffsetFraction = -.6454f / 180),
        WaveLayerSpec(3.72f / 180f, periodSeconds = .8493, phaseRadians = -.288916),
    )
    private val sinePaths = waves.map { wave ->
        val points = WaveSamples(wave, 180f, 180f, .01f)
        Path().apply {
            moveTo(points.coordinates[0], points.coordinates[1])
            for (i in 2 until points.coordinates.size step 2) lineTo(points.coordinates[i], points.coordinates[i + 1])
            lineTo(points.endX, 360f); lineTo(0f, 360f); close()
        }
    }
    private var time = 8.0
    private var anchor: Long? = null
    var isPlaying = true
        set(value) { field = value; anchor = null; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPlaying) {
            val now = System.nanoTime()
            anchor?.let { time += (now - it) / 1e9 }
            anchor = now
        }
        val gap = 12f * resources.displayMetrics.density
        val cell = (width - gap) / 2f
        for (side in 0..1) {
            val save = canvas.save()
            canvas.translate(side * (cell + gap), 0f)
            canvas.scale(cell / 180f, height / 180f)
            canvas.clipRect(0f, 0f, 180f, 180f)
            canvas.drawColor(Color.WHITE)
            for ((index, wave) in waves.withIndex()) {
                paint.color = 0xFF00B9F2.toInt()
                paint.alpha = (wave.opacity * 255).toInt()
                if (side == 0) {
                    val shift = 180 / wave.periodSeconds * time + (wave.phaseRadians + PI / 2) * 180 / (2 * PI)
                    val origin = (-(shift % 180) - 180).toFloat()
                    val crest = 90 + wave.levelOffsetFraction * 180 - wave.amplitudeFraction * 180
                    val trough = 90 + wave.levelOffsetFraction * 180 + wave.amplitudeFraction * 180
                    bezier.reset(); bezier.moveTo(origin, crest)
                    var x = origin
                    while (x < 360) {
                        bezier.cubicTo(x + 32.4f, crest, x + 57.6f, trough, x + 90, trough)
                        bezier.cubicTo(x + 122.4f, trough, x + 147.6f, crest, x + 180, crest)
                        x += 180
                    }
                    bezier.lineTo(x, 360f); bezier.lineTo(origin, 360f); bezier.close()
                    canvas.drawPath(bezier, paint)
                } else {
                    val layer = canvas.save()
                    canvas.translate(wave.translationX(180f, time), 90f)
                    canvas.drawPath(sinePaths[index], paint)
                    canvas.restoreToCount(layer)
                }
            }
            canvas.restoreToCount(save)
        }
        if (isPlaying && isAttachedToWindow) postInvalidateOnAnimation()
    }
}
