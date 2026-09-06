package com.jameeli.thykra.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.jameeli.thykra.ui.theme.darkroomScheme
import com.jameeli.thykra.ui.theme.paperScheme

/**
 * Design part 4 §13.
 *
 * Glance gives boxes, rows, columns, text and images — no custom drawing and no fonts of
 * its own. So the Editions look on a home screen comes from colour, spacing and the plate
 * rule, and nothing else: no Archivo, no numerals, no Stamp, no italic clay phrase.
 *
 * Both schemes cost nothing here because Glance follows the system theme itself, which is
 * also why a widget ignores the in-app Paper/Darkroom preference: a home screen is the
 * system's surface, not the app's.
 */
@Composable
fun ThykraGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(light = paperScheme, dark = darkroomScheme),
        content = content,
    )
}

/** Text sizes, in the three steps Glance has room for. */
object WidgetType {
    const val TITLE_SP = 13
    const val BODY_SP = 12.5f
    const val META_SP = 11
}
