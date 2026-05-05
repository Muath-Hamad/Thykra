package com.jameeli.thykra.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.jameeli.thykra.ui.theme.ThykraColors
import android.content.Context

/**
 * Glance widget that shows a small feed of recent reactions / comments across an album.
 *
 * Stub — full implementation lands in a follow-up commit.
 */
class RecentActivityWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            RecentActivityContent()
        }
    }

    @Composable
    private fun RecentActivityContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(ThykraColors.WarmWhite))
                .padding(16.dp)
        ) {
            Text(text = "Thykra")
        }
    }
}
