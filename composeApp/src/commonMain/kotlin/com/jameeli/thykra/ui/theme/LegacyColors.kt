package com.jameeli.thykra.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The pre-Editions palette, remapped onto the Wanderlust tokens so the screens that have
 * not been rebuilt yet (build steps 03-11) render in the new colours immediately rather
 * than sitting in the old ones until their step lands.
 *
 * Nothing new may use this. New code names a [androidx.compose.material3.ColorScheme]
 * role or a field on [ExtendedColors]; a hex, or one of these names, in a screen file is
 * a review failure. Every reference disappears as its screen is rebuilt.
 */
@Deprecated(
    "Pre-Editions palette. Use MaterialTheme.colorScheme or MaterialTheme.thykra instead.",
    level = DeprecationLevel.WARNING,
)
object ThykraColors {
    /** -> `colorScheme.primary` */
    val SkyBlue = Paper.accent
    /** -> `colorScheme.inversePrimary`, or `primary` in Darkroom */
    val OceanBlue = Darkroom.accent
    /** -> `colorScheme.tertiary` (clay) */
    val SunriseOrange = Paper.warm
    /** -> `colorScheme.surfaceVariant` */
    val Sandy = Paper.bgSunken
    /** -> `colorScheme.surface` */
    val WarmWhite = Paper.bg
    /** -> `colorScheme.onSurface` */
    val DeepNavy = Paper.text
    /** -> `thykra.textMeta` */
    val MutedSlate = Paper.text3
    /** -> `colorScheme.error` */
    val SoftRed = Paper.bad
}
