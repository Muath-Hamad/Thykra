package com.jameeli.thykra.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Home-screen widget receiver that hosts [LatestPhotoWidget].
 *
 * Registered in the manifest with [latest_photo_widget_info] metadata.
 */
class LatestPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestPhotoWidget()
}
