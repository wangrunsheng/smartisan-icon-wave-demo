package com.russell.wavedemo.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.Button
import kotlin.math.roundToInt

/** Small shared palette and native controls; used by the View demonstration. */
internal class DemoStyle(private val context: Context) {
    val ink = 0xFF1E2B2A.toInt()
    val accent = 0xFF236959.toInt()
    val muted = 0xFF73807B.toInt()
    val surface = 0xFFF7F9F6.toInt()

    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    fun button(resource: Int, primary: Boolean) = Button(context).apply {
        setText(resource)
        textSize = 15f
        isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (primary) Color.WHITE else accent)
        stateListAnimator = null
        elevation = 0f
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(12), 0, dp(12), 0)
        background = RippleDrawable(
            ColorStateList.valueOf(0x20236959),
            shape(if (primary) accent else 0xFFE6EEE8.toInt(), 15),
            null,
        )
    }

}
