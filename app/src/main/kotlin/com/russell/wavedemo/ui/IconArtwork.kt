package com.russell.wavedemo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.russell.wavedemo.R

/** Rasterizes the original color and muted artwork once, outside the animation loop. */
internal object IconArtwork {
    private const val CACHE_SIZE = 1080
    private const val REFERENCE_SIZE = 180f

    fun create(context: Context, muted: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(CACHE_SIZE, CACHE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply {
            scale(CACHE_SIZE / REFERENCE_SIZE, CACHE_SIZE / REFERENCE_SIZE)
        }
        val icon = requireNotNull(context.getDrawable(R.drawable.chrome)).mutate()
        icon.setBounds(24, 20, 160, 156)
        if (muted) {
            // Preserve the initial demo's luminance structure with one transform for all
            // pixels. The ring has no separate tint and the icon receives no dark overlay.
            icon.colorFilter = ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        .10f, .20f, .04f, 0f, 152f,
                        .10f, .20f, .04f, 0f, 158f,
                        .10f, .20f, .04f, 0f, 154f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
        icon.draw(canvas)
        return bitmap
    }
}
