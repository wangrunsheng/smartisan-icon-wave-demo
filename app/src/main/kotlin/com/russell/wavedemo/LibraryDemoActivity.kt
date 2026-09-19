package com.russell.wavedemo

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.russell.wave.view.WaveRevealLayout
import com.russell.wavedemo.ui.CurveComparisonView
import com.russell.wavedemo.ui.DemoStyle

/** Live examples: one color, a ViewGroup, and a user-selected photo use the same library. */
class LibraryDemoActivity : Activity() {
    private lateinit var card: WaveRevealLayout
    private lateinit var fill: WaveRevealLayout
    private lateinit var comparison: CurveComparisonView
    private var playing = true
    private var level = .55f
    private val style by lazy { DemoStyle(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        level = savedInstanceState?.getFloat("level", .55f) ?: .55f
        playing = savedInstanceState?.getBoolean("playing", true) ?: true
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(style.dp(22), style.dp(16), style.dp(22), style.dp(24))
            setBackgroundColor(style.surface)
        }
        fun label(text: String, size: Float = 14f) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(style.ink)
                setPadding(0, style.dp(10), 0, style.dp(8))
            })
        }
        label("Wave Reveal · 通用组件", 24f)
        label("同参数对比：左 Bézier / 右 Sine")
        comparison = CurveComparisonView(this)
        root.addView(comparison, LinearLayout.LayoutParams(-1, style.dp(145)))
        label("同一个模板：纯色 / 日期 + 内容组合")
        val row = LinearLayout(this)
        fill = WaveRevealLayout(this).apply {
            cornerRadiusPx = style.dp(18).toFloat()
            setBackdrop(View(this@LibraryDemoActivity).apply { setBackgroundColor(Color.WHITE) })
            setContent(View(this@LibraryDemoActivity).apply { setBackgroundColor(0xFF00B9F2.toInt()) })
        }
        card = WaveRevealLayout(this).apply {
            cornerRadiusPx = style.dp(18).toFloat()
            setBackdrop(View(this@LibraryDemoActivity).apply { setBackgroundColor(Color.WHITE) })
            setContent(LinearLayout(this@LibraryDemoActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(0xFF207BEA.toInt())
                addView(TextView(this@LibraryDemoActivity).apply {
                    text = "✦"
                    textSize = 42f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@LibraryDemoActivity).apply {
                    text = "任意控件组合"
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(Color.WHITE)
                })
            })
            setOverlay(TextView(this@LibraryDemoActivity).apply {
                text = "19"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFF153A55.toInt())
                setPadding(style.dp(14), style.dp(10), 0, 0)
            })
        }
        row.addView(fill, LinearLayout.LayoutParams(0, style.dp(170), 1f).apply { marginEnd = style.dp(12) })
        row.addView(card, LinearLayout.LayoutParams(0, style.dp(170), 1f))
        root.addView(row)
        label("揭示进度（波动与进度独立）")
        root.addView(SeekBar(this).apply {
            max = 1000
            progress = (level * 1000).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    level = progress / 1000f
                    fill.progress = level
                    card.progress = level
                }
            })
        }, LinearLayout.LayoutParams(-1, style.dp(48)))
        root.addView(style.button(if (playing) R.string.pause_waves else R.string.resume_waves, primary = true).apply {
            minimumHeight = style.dp(48)
            layoutParams = LinearLayout.LayoutParams(-1, style.dp(48)).apply { topMargin = style.dp(8) }
            setOnClickListener {
                playing = !playing
                setText(if (playing) R.string.pause_waves else R.string.resume_waves)
                applyPlayback(playing)
            }
        })
        root.addView(style.button(R.string.choose_photo, primary = false).apply {
            minimumHeight = style.dp(48)
            layoutParams = LinearLayout.LayoutParams(-1, style.dp(48)).apply { topMargin = style.dp(8) }
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, 19)
            }
        })
        label("可拖到 0% / 100% 检查边缘。日期始终显示，内容被波形揭示。", 12f)
        fill.progress = level
        card.progress = level
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun applyPlayback(active: Boolean) {
        fill.isRunning = active
        card.isRunning = active
        comparison.isPlaying = active
    }

    override fun onResume() { super.onResume(); applyPlayback(playing) }
    override fun onPause() { applyPlayback(false); super.onPause() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("playing", playing)
        outState.putFloat("level", level)
    }

    @Deprecated("Platform callback retained for the dependency-free Android 8 demo.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 19 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
            options.inJustDecodeBounds = false
            options.inSampleSize = sample
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (bitmap != null) card.setContent(ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
                contentDescription = "19 日选择的照片"
            })
        } catch (_: java.io.IOException) {
            android.widget.Toast.makeText(this, "无法读取该照片", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            android.widget.Toast.makeText(this, "照片访问权限已失效", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
