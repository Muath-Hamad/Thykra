package com.jameeli.thykra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jameeli.thykra.ui.theme.ThykraColors

@Composable
actual fun VideoPlayer(url: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(ThykraColors.DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Text("Video playback not yet supported on iOS", color = Color.White.copy(alpha = 0.6f))
    }
}
