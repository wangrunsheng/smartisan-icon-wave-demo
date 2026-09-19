package com.russell.wave.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.russell.wave.core.WaveLayerSpec
import com.russell.wave.core.WaveRevealSpec
import com.russell.wave.core.WaveSamples
import kotlin.math.roundToInt

/**
 * Reveals one live View tree through cached sine-wave masks.
 *
 * Use [setContent] for any normal View/ViewGroup, [setBackdrop] for content below
 * the effect, and [setOverlay] for fixed labels above it. Each slot is laid out
 * normally and exists exactly once, retaining its state and accessibility tree.
 * Visual masking does not change hit testing; disable input yourself if required.
 * SurfaceView and other separately composed surfaces are not supported.
 *
 * All setters and playback calls must run on the UI thread, like ordinary Views.
 */
class WaveRevealLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val bounds = RectF()
    private val outerClip = Path()
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val clearMask = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
    }
    private val destinationIn = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private var paths: List<Pair<WaveLayerSpec, Path>> = emptyList()
    private var content: View? = null
    private var backdrop: View? = null
    private var overlay: View? = null
    private var lastFrameNanos: Long? = null
    private var elapsedSeconds = 0.0
    private var visibleToUser = false

    var spec = WaveRevealSpec()
        set(value) {
            field = value
            rebuildGeometry()
            invalidate()
        }

    /** Water-level progress, clamped to [0, 1]. No automatic reset or looping of progress. */
    var progress = 0.5f
        set(value) {
            require(value.isFinite())
            val next = value.coerceIn(0f, 1f)
            if (field == 0f || field == 1f || next == 0f || next == 1f) lastFrameNanos = null
            field = next
            invalidate()
        }

    /** Independent wave clock. Set [isRunning] false to drive time externally. */
    var timeSeconds: Double
        get() = elapsedSeconds
        set(value) {
            require(value.isFinite())
            elapsedSeconds = value
            lastFrameNanos = null
            invalidate()
        }

    var isRunning = true
        set(value) {
            field = value
            lastFrameNanos = null
            invalidate()
        }

    var speed = 1.0
        set(value) {
            require(value.isFinite() && value >= 0.0)
            field = value
            lastFrameNanos = null
            invalidate()
        }

    /** Radius in physical pixels; the same outer clip applies to all three slots. */
    var cornerRadiusPx = 0f
        set(value) {
            require(value.isFinite() && value >= 0f)
            field = value
            rebuildOuterClip()
            invalidate()
        }

    fun setContent(view: View) {
        content = replaceSlot(content, view)
        overlay?.bringToFront()
    }

    fun setBackdrop(view: View?) {
        backdrop = replaceSlot(backdrop, view)
        content?.bringToFront()
        overlay?.bringToFront()
    }

    fun setOverlay(view: View?) {
        overlay = replaceSlot(overlay, view)
        overlay?.bringToFront()
    }

    private fun replaceSlot(previous: View?, next: View?): View? {
        if (previous === next) return previous
        require(next?.parent == null) { "The supplied View already has a parent." }
        previous?.let { removeView(it) }
        next?.let { addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)) }
        return next
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(0f, 0f, w.toFloat(), h.toFloat())
        rebuildGeometry()
        rebuildOuterClip()
    }

    private fun rebuildOuterClip() {
        outerClip.reset()
        outerClip.addRoundRect(bounds, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }

    private fun rebuildGeometry() {
        if (width <= 0 || height <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        paths = spec.waves.map { wave ->
            val samples = WaveSamples(wave, w, h)
            wave to Path().apply {
                moveTo(samples.coordinates[0], samples.coordinates[1])
                for (index in 2 until samples.coordinates.size step 2) {
                    lineTo(samples.coordinates[index], samples.coordinates[index + 1])
                }
                val bottom = h + 2 * spec.margin(w, h)
                lineTo(samples.endX, bottom)
                lineTo(0f, bottom)
                close()
            }
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        visibleToUser = isVisible
        lastFrameNanos = null
        if (isVisible) invalidate()
    }

    override fun onDetachedFromWindow() {
        lastFrameNanos = null
        super.onDetachedFromWindow()
    }

    override fun draw(canvas: Canvas) {
        val saved = canvas.save()
        canvas.clipPath(outerClip)
        super.draw(canvas)
        canvas.restoreToCount(saved)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        val outerSave = canvas.save()
        canvas.clipPath(outerClip)
        val animate = isRunning && speed > 0 && visibleToUser && isAttachedToWindow &&
            progress > 0f && progress < 1f
        if (animate) {
            val now = System.nanoTime()
            val previous = lastFrameNanos
            if (previous != null) elapsedSeconds += (now - previous) / 1_000_000_000.0 * speed
            lastFrameNanos = now
        } else {
            lastFrameNanos = null
        }

        backdrop?.let { drawChild(canvas, it, drawingTime) }
        content?.let { child ->
            // Isolate the entire content subtree so the mask multiplies group alpha,
            // rather than applying opacity separately to overlapping child Views.
            val contentSave = canvas.saveLayerAlpha(bounds, (spec.contentOpacity * 255).roundToInt())
            drawChild(canvas, child, drawingTime)
            val maskSave = canvas.saveLayer(bounds, destinationIn)
            // Explicitly paint transparent pixels across the full mask extent.
            // On older HWUI backends an untouched area can otherwise be omitted
            // from the layer's restore coverage, leaving content visible above it.
            canvas.drawRect(bounds, clearMask)
            maskPaint.alpha = (spec.baseOpacity * 255).roundToInt()
            canvas.drawRect(bounds, maskPaint)
            val baseline = spec.baseline(progress, width.toFloat(), height.toFloat())
            for ((wave, path) in paths) {
                val waveSave = canvas.save()
                canvas.translate(wave.translationX(width.toFloat(), timeSeconds), baseline)
                maskPaint.alpha = (wave.opacity * 255).roundToInt()
                canvas.drawPath(path, maskPaint)
                canvas.restoreToCount(waveSave)
            }
            canvas.restoreToCount(maskSave)
            canvas.restoreToCount(contentSave)
        }
        overlay?.let { drawChild(canvas, it, drawingTime) }
        canvas.restoreToCount(outerSave)
        if (animate) postInvalidateOnAnimation()
    }
}
