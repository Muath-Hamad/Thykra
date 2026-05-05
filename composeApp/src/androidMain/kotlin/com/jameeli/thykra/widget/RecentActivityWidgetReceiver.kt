package com.jameeli.thykra.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Home-screen widget receiver that hosts [RecentActivityWidget].
 *
 * Registered in the manifest with [recent_activity_widget_info] metadata.
 */
class RecentActivityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentActivityWidget()
}
