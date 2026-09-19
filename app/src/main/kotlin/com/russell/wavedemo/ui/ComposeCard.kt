package com.russell.wavedemo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.russell.wave.compose.WaveReveal
import com.russell.wave.compose.rememberWaveTime

/** Ordinary composable content; no AndroidView or snapshot is used by the effect. */
@Composable
internal fun ComposeCard(progress: Float, playing: Boolean, photo: ImageBitmap? = null) {
    val time = rememberWaveTime(playing && progress > 0f && progress < 1f)
    WaveReveal(
        progress = progress,
        timeSeconds = { time.value },
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        backdrop = { Box(Modifier.fillMaxSize().background(Color.White)) },
        overlay = {
            BasicText("19", Modifier.padding(14.dp),
                style = TextStyle(color = Color(0xFF153A55), fontSize = 24.sp, fontWeight = FontWeight.Bold))
        },
    ) {
        if (photo != null) {
            Image(photo, "卡片照片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else Box(Modifier.fillMaxSize().background(Color(0xFF00B9F2)), contentAlignment = Alignment.Center) {
            BasicText("✦  Wave Reveal", style = TextStyle(color = Color.White, fontSize = 22.sp))
        }
    }
}
