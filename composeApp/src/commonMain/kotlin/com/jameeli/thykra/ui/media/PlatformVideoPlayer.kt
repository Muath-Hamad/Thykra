package com.jameeli.thykra.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(url: String, isActive: Boolean, modifier: Modifier = Modifier)
