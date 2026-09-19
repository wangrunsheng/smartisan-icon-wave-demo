package com.russell.wavedemo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import com.russell.wavedemo.R
import com.russell.wavedemo.motion.PlaybackClock
import com.russell.wavedemo.motion.WaterLevel
import com.russell.wavedemo.motion.WaveSpec

/** Composites two independently translating Bézier masks over cached icon artwork. */
class WaveView(context: Context) : View(context) {
    private val clock = PlaybackClock()
    private val colorArtwork = IconArtwork.create(context, muted = false)
    private val mutedArtwork = IconArtwork.create(context, muted = true)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.7f
    }
    private val wavePath = Path()
    private val iconBounds = RectF(0f, 0f, 180f, 180f)
    private val iconClip = Path().apply { addCircle(92f, 88f, 68f, Path.Direction.CW) }
    private val rearDash = DashPathEffect(floatArrayOf(2f, 1f), 0f)
    private var hostActive = false

    var isPlaying = true
        set(value) {
            field = value
            clock.resetFrameAnchor()
            invalidate()
        }
    var isSlow = false
        set(value) {
            field = value
            clock.resetFrameAnchor()
        }
    var showOutlines = false
        set(value) {
            field = value
            invalidate()
        }
    var isManual = false
        set(value) {
            field = value
            invalidate()
        }
    var progress = 58
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }
    var seconds: Double
        get() = clock.seconds
        set(value) {
            clock.seconds = value.coerceIn(WaterLevel.START_SECONDS, WaterLevel.END_SECONDS)
            clock.resetFrameAnchor()
            invalidate()
        }

    init {
        contentDescription = context.getString(R.string.wave_description)
    }

    fun setHostActive(active: Boolean) {
        hostActive = active
        clock.resetFrameAnchor()
        if (active) invalidate()
    }

    override fun onDetachedFromWindow() {
        clock.resetFrameAnchor()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPlaying && hostActive) clock.advance(System.nanoTime(), if (isSlow) 0.25 else 1.0)
        val size = minOf(width, height) * 0.94f
        val saveCount = canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / 180f, size / 180f)
        canvas.drawBitmap(mutedArtwork, null, iconBounds, bitmapPaint)

        val waterline = if (isManual) WaterLevel.fromProgress(progress) else WaterLevel.at(seconds)
        drawLayer(canvas, WaveSpec.Rear, waterline)
        drawLayer(canvas, WaveSpec.Front, waterline)
        if (showOutlines) drawOutlines(canvas, waterline)
        canvas.restoreToCount(saveCount)

        if (isPlaying && hostActive && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private fun drawLayer(canvas: Canvas, wave: WaveSpec, waterline: Float) {
        buildPath(wave, waterline, close = true)
        val saveCount = canvas.save()
        canvas.clipPath(wavePath)
        bitmapPaint.alpha = wave.opacity
        canvas.drawBitmap(colorArtwork, null, iconBounds, bitmapPaint)
        canvas.restoreToCount(saveCount)
        bitmapPaint.alpha = 255
    }

    private fun drawOutlines(canvas: Canvas, waterline: Float) {
        val saveCount = canvas.save()
        canvas.clipPath(iconClip)
        outlinePaint.color = 0xFFFFA844.toInt()
        outlinePaint.pathEffect = rearDash
        buildPath(WaveSpec.Rear, waterline, close = false)
        canvas.drawPath(wavePath, outlinePaint)
        outlinePaint.color = 0xFF26DFE7.toInt()
        outlinePaint.pathEffect = null
        buildPath(WaveSpec.Front, waterline, close = false)
        canvas.drawPath(wavePath, outlinePaint)
        canvas.restoreToCount(saveCount)
    }

    private fun buildPath(wave: WaveSpec, waterline: Float, close: Boolean) {
        val origin = wave.originAt(seconds)
        val half = WaveSpec.WAVELENGTH / 2f
        val handle = half * WaveSpec.HANDLE_RATIO
        val crest = waterline + wave.levelOffset - wave.amplitude
        val trough = waterline + wave.levelOffset + wave.amplitude
        wavePath.reset()
        wavePath.moveTo(origin, crest)
        var x = origin
        while (x < 360f) {
            // Horizontal handles give each crest and trough a flat, continuous tangent.
            wavePath.cubicTo(x + handle, crest, x + half - handle, trough, x + half, trough)
            wavePath.cubicTo(
                x + half + handle, trough,
                x + WaveSpec.WAVELENGTH - handle, crest,
                x + WaveSpec.WAVELENGTH, crest,
            )
            x += WaveSpec.WAVELENGTH
        }
        if (close) {
            wavePath.lineTo(x, 180f)
            wavePath.lineTo(origin, 180f)
            wavePath.close()
        }
    }
}
