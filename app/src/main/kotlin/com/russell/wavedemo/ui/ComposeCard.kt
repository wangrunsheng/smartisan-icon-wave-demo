package com.russell.wavedemo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russell.wave.compose.WaveReveal
import com.russell.wave.compose.waveReveal
import com.russell.wave.core.WaveRevealDefaults

/** Two public entry points, each rendering its own content once with an automatic clock. */
@Composable
internal fun ComposeCard(progress: Float, playing: Boolean, photo: ImageBitmap? = null) {
    val shape = RoundedCornerShape(18.dp)
    val spec = remember {
        WaveRevealDefaults.DoubleWave.copy(
            waves = listOf(
                WaveRevealDefaults.RearWave.copy(opacity = .25f),
                WaveRevealDefaults.FrontWave,
            ),
        )
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WaveReveal(
            progress = progress,
            modifier = Modifier.weight(1f).fillMaxSize(),
            spec = spec,
            running = playing,
            shape = shape,
            backdrop = { Box(Modifier.fillMaxSize().background(Color.White)) },
            overlay = { CardNumber() },
        ) {
            CardContent(photo, "容器", Modifier.fillMaxSize())
        }
        // The existing card owns its background and label; only its content gets a mask.
        Box(Modifier.weight(1f).fillMaxSize().clip(shape).background(Color.White)) {
            CardContent(
                photo,
                "Modifier",
                Modifier.fillMaxSize().waveReveal(progress, spec = spec, running = playing),
            )
            CardNumber()
        }
    }
}

@Composable
private fun CardNumber() {
    BasicText(
        "19",
        Modifier.padding(14.dp),
        style = TextStyle(color = Color(0xFF153A55), fontSize = 24.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun CardContent(photo: ImageBitmap?, label: String, modifier: Modifier) {
    if (photo != null) {
        Image(photo, "卡片照片", modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Color(0xFF00B9F2)), contentAlignment = Alignment.Center) {
            BasicText(label, style = TextStyle(color = Color.White, fontSize = 22.sp))
        }
    }
}
