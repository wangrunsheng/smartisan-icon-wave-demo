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

/** Composites two independently translating sine-wave masks over cached icon artwork. */
class WaveView(context: Context) : View(context) {
    private val clock = PlaybackClock()
    private val colorArtwork = IconArtwork.create(context, muted = false)
    private val mutedArtwork = IconArtwork.create(context, muted = true)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.7f
    }
    private val rearGeometry = WaveGeometry(WaveSpec.Rear)
    private val frontGeometry = WaveGeometry(WaveSpec.Front)
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
        drawLayer(canvas, rearGeometry, waterline)
        drawLayer(canvas, frontGeometry, waterline)
        if (showOutlines) drawOutlines(canvas, waterline)
        canvas.restoreToCount(saveCount)

        if (isPlaying && hostActive && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private fun drawLayer(canvas: Canvas, geometry: WaveGeometry, waterline: Float) {
        val dx = geometry.wave.translationAt(seconds)
        val saveCount = canvas.save()
        canvas.translate(dx, waterline)
        canvas.clipPath(geometry.fill)
        // The clip stays in device space. Undo the translation so only the water
        // moves, while the icon remains anchored to its original bounds.
        canvas.translate(-dx, -waterline)
        bitmapPaint.alpha = geometry.wave.opacity
        canvas.drawBitmap(colorArtwork, null, iconBounds, bitmapPaint)
        canvas.restoreToCount(saveCount)
        bitmapPaint.alpha = 255
    }

    private fun drawOutlines(canvas: Canvas, waterline: Float) {
        val saveCount = canvas.save()
        canvas.clipPath(iconClip)
        outlinePaint.color = 0xFFFFA844.toInt()
        outlinePaint.pathEffect = rearDash
        drawOutline(canvas, rearGeometry, waterline)
        outlinePaint.color = 0xFF26DFE7.toInt()
        outlinePaint.pathEffect = null
        drawOutline(canvas, frontGeometry, waterline)
        canvas.restoreToCount(saveCount)
    }

    private fun drawOutline(canvas: Canvas, geometry: WaveGeometry, waterline: Float) {
        val saveCount = canvas.save()
        canvas.translate(geometry.wave.translationAt(seconds), waterline)
        canvas.drawPath(geometry.outline, outlinePaint)
        canvas.restoreToCount(saveCount)
    }

    /**
     * Two periods cover the viewport throughout a one-period translation.
     * Geometry is sampled once per View instance, including the fixed phase and
     * level offset. Frames only translate these immutable paths; they never
     * evaluate sine or rebuild vertices. A separate open path serves outlines.
     */
    private class WaveGeometry(val wave: WaveSpec) {
        val outline = Path().apply {
            moveTo(0f, wave.offsetAt(0f, 0.0))
            // Half-unit sampling keeps the maximum interpolation error below
            // 0.00022 reference units for the configured amplitudes.
            for (sample in 1..720) {
                val x = sample * 0.5f
                lineTo(x, wave.offsetAt(x, 0.0))
            }
        }
        val fill = Path(outline).apply {
            // Keep the closing edge below the icon even at the lowest waterline.
            lineTo(360f, 360f)
            lineTo(0f, 360f)
            close()
        }
    }
}
