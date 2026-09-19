package com.russell.wavedemo

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.russell.wavedemo.ui.DemoStyle
import com.russell.wavedemo.ui.WaveView

/** Owns screen controls and lifecycle; wave geometry remains independent of the activity. */
class MainActivity : Activity() {
    private lateinit var style: DemoStyle
    private lateinit var wave: WaveView
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var speedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        style = DemoStyle(this)
        wave = WaveView(this)
        restorePlayback(savedInstanceState)
        setContentView(createScreen())
        updateControls()
    }

    private fun createScreen(): View {
        val root = column().apply {
            setPadding(style.dp(28), style.dp(28), style.dp(28), style.dp(24))
            setBackgroundColor(style.surface)
        }
        root.addView(style.text(R.string.eyebrow, 11f).apply {
            letterSpacing = .16f
            setTextColor(style.muted)
        })
        root.addView(style.text(R.string.app_name, 30f).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, style.dp(10), 0, 0)
        })
        root.addView(style.text(R.string.subtitle, 14f).apply {
            setTextColor(style.muted)
            setPadding(0, style.dp(8), 0, 0)
        })
        // Keep the controls reachable in landscape and with larger system text settings.
        val previewHeight = maxOf(240, resources.configuration.screenHeightDp - 490)
        root.addView(wave, LinearLayout.LayoutParams(MATCH_PARENT, style.dp(previewHeight)))
        status = TextView(this).apply {
            textSize = 12f
            setTextColor(style.muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, style.dp(20))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        root.addView(status)
        root.addView(createPlaybackButtons())
        root.addView(createSettings())
        root.addView(style.text(R.string.footer, 11f).apply {
            setTextColor(style.muted)
            gravity = Gravity.CENTER
            setPadding(0, style.dp(18), 0, 0)
        })
        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(style.surface)
            addView(root)
        }
    }

    private fun createPlaybackButtons(): View = LinearLayout(this).apply {
        setPadding(0, 0, 0, style.dp(22))
        playButton = style.button(R.string.pause, primary = true).apply {
            setOnClickListener {
                wave.isPlaying = !wave.isPlaying
                updateControls()
            }
        }
        speedButton = style.button(R.string.slow, primary = false).apply {
            setOnClickListener {
                wave.isSlow = !wave.isSlow
                updateControls()
            }
        }
        addView(playButton, LinearLayout.LayoutParams(0, style.dp(50), 1f).apply {
            marginEnd = style.dp(10)
        })
        addView(speedButton, LinearLayout.LayoutParams(0, style.dp(50), 1f))
    }

    private fun createSettings(): View = column().apply {
        setPadding(style.dp(18), style.dp(8), style.dp(18), style.dp(12))
        background = style.shape(Color.WHITE, 20)
        val outlines = style.toggle(R.string.outlines).apply {
            isChecked = wave.showOutlines
            setOnCheckedChangeListener { _, checked -> wave.showOutlines = checked }
        }
        addView(outlines, rowParams())
        addView(View(this@MainActivity).apply { setBackgroundColor(0xFFEDF0EC.toInt()) },
            LinearLayout.LayoutParams(MATCH_PARENT, style.dp(1)))
        val slider = SeekBar(this@MainActivity).apply {
            max = 100
            progress = wave.progress
            isEnabled = wave.isManual
            contentDescription = getString(R.string.water_level)
            progressTintList = ColorStateList.valueOf(style.accent)
            thumbTintList = ColorStateList.valueOf(style.accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    wave.progress = progress
                }
            })
        }
        val manual: Switch = style.toggle(R.string.manual_level).apply {
            isChecked = wave.isManual
            setOnCheckedChangeListener { _, checked ->
                wave.isManual = checked
                slider.isEnabled = checked
                updateControls()
            }
        }
        addView(manual, rowParams())
        addView(slider, LinearLayout.LayoutParams(MATCH_PARENT, style.dp(44)))
    }

    private fun updateControls() {
        playButton.setText(if (wave.isPlaying) R.string.pause else R.string.resume)
        speedButton.setText(if (wave.isSlow) R.string.normal else R.string.slow)
        status.text = getString(
            R.string.status,
            getString(if (wave.isPlaying) R.string.playing else R.string.paused),
            if (wave.isSlow) "0.25×" else "1×",
            getString(if (wave.isManual) R.string.custom else R.string.automatic),
        )
    }

    override fun onResume() {
        super.onResume()
        wave.setHostActive(true)
    }

    override fun onPause() {
        wave.setHostActive(false)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("playing", wave.isPlaying)
        outState.putBoolean("slow", wave.isSlow)
        outState.putBoolean("outlines", wave.showOutlines)
        outState.putBoolean("manual", wave.isManual)
        outState.putInt("progress", wave.progress)
        outState.putDouble("seconds", wave.seconds)
    }

    private fun restorePlayback(state: Bundle?) {
        if (state == null) return
        wave.isPlaying = state.getBoolean("playing", true)
        wave.isSlow = state.getBoolean("slow")
        wave.showOutlines = state.getBoolean("outlines")
        wave.isManual = state.getBoolean("manual")
        wave.progress = state.getInt("progress", 58)
        wave.seconds = state.getDouble("seconds", 8.0)
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun rowParams() = LinearLayout.LayoutParams(MATCH_PARENT, style.dp(54))

    private companion object {
        const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
    }
}
