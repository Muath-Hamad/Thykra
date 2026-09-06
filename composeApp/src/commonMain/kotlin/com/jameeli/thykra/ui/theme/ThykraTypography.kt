package com.jameeli.thykra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Every line height is absolute sp (Compose has no unitless multiplier) and every slot
 * trims the same way, so a measurement here matches the CSS on the web.
 */
private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun slot(
    family: FontFamily,
    weight: FontWeight,
    size: Float,
    line: Float,
    tracking: Float = 0f,
    features: String? = null,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = lineHeightStyle,
    fontFeatureSettings = features,
)

/**
 * The day numeral. Not a Material slot — carried on [LocalNumeralStyle] and read as
 * `MaterialTheme.numeralStyle`. Tabular figures so `01` and `11` are the same width.
 */
val LocalNumeralStyle = staticCompositionLocalOf {
    slot(FontFamily.SansSerif, FontWeight.Bold, 40f, 40f, -0.04f, "tnum")
}

val MaterialTheme.numeralStyle: TextStyle
    @Composable @ReadOnlyComposable get() = LocalNumeralStyle.current

@Immutable
data class ThykraTypographySet(
    val typography: Typography,
    val numeral: TextStyle,
)

/**
 * The default instance. Sizes are the web's lower clamp bound.
 *
 * `compact` is chosen below 380 dp of window width: display slots step down one
 * (44 → 34, 34 → 28, 28 → 23) so a trip title still fits on two lines. Nothing hides.
 *
 * `arabic` is chosen when the app locale is `ar`: display and headline slots have no
 * Archivo to fall back on, so they switch to Readex Pro 700 (600 for headlines) at
 * 0.92x size and 1.32x line height, tracking goes to zero everywhere, and labelSmall
 * loses its tracking (the `Label` composable also stops uppercasing it).
 */
fun thykraTypography(
    fonts: ThykraFonts,
    compact: Boolean = false,
    arabic: Boolean = false,
    fontScale: Float = 1f,
): ThykraTypographySet {
    // Archivo is Latin-only, so Arabic display and headline slots fall back to Readex.
    val display = if (arabic) fonts.text else fonts.display
    val text = fonts.text

    // Sizes are sp, so the platform already scaled them. Display slots and the numeral
    // divide the excess back out so they stop growing past 1.5x.
    val displayScale = (fontScale.coerceAtMost(DisplayFontScaleCap) / fontScale)
        .coerceIn(0.01f, 1f)

    // Arabic re-sets the display and headline metrics; Latin keeps the drawn values.
    fun d(size: Float, line: Float, tracking: Float) = if (arabic) {
        slot(display, FontWeight.Bold, size * 0.92f * displayScale, line * 1.32f * displayScale, 0f)
    } else {
        slot(display, FontWeight.Bold, size * displayScale, line * displayScale, tracking)
    }

    fun h(size: Float, line: Float, tracking: Float) = if (arabic) {
        slot(display, FontWeight.SemiBold, size * 0.92f, line * 1.32f, 0f)
    } else {
        slot(display, FontWeight.SemiBold, size, line, tracking)
    }

    // Compact steps the three display slots down one, and nothing else.
    val displayLargeSize = if (compact) 34f else 44f
    val displayLargeLine = if (compact) 34f else 43f
    val displayMediumSize = if (compact) 28f else 34f
    val displayMediumLine = if (compact) 29f else 34f
    val displaySmallSize = if (compact) 23f else 28f
    val displaySmallLine = if (compact) 25f else 29f

    val typography = Typography(
        // Trip masthead title, invite title, landing headline.
        displayLarge = d(displayLargeSize, displayLargeLine, -0.02f),
        // Greeting on Trips, invite states, empty-state headlines.
        displayMedium = d(displayMediumSize, displayMediumLine, -0.02f),
        // Recap card title, Activity screen heading.
        displaySmall = d(displaySmallSize, displaySmallLine, -0.015f),

        // Sheet titles — one step under display, because a sheet title sits lower and closer.
        headlineLarge = h(26f, 28f, -0.01f),
        // Settings section headings, dialog titles.
        headlineMedium = h(23f, 25f, -0.01f),
        // Chapter header date, pinned top-bar title once the masthead scrolls off.
        headlineSmall = h(20f, 24f, -0.005f),

        // Trip card title, list group headers.
        titleLarge = slot(text, FontWeight.SemiBold, 18f, 23.4f),
        // List item primary text — member rows, activity actor, comment author, dock summary.
        titleMedium = slot(text, FontWeight.SemiBold, 16f, 22f),
        // Pinned chapter hairline bar, nav-bar labels, dense sheet rows.
        titleSmall = slot(text, FontWeight.SemiBold, 14f, 20f),

        // Invite description, empty-state body, text-field input.
        bodyLarge = slot(text, FontWeight.Normal, 16f, 24f),
        // Everything unmarked, comments.
        bodyMedium = slot(text, FontWeight.Normal, 15f, 22f),
        // List secondary lines, helper text under inputs.
        bodySmall = slot(text, FontWeight.Normal, 13.5f, 19.5f),

        // Buttons, segmented control, tabs. Readex, not Archivo — Archivo Expanded at
        // 15 sp inside a 48 dp button eats 25% more width and stops fitting at 360 dp.
        labelLarge = slot(text, FontWeight.SemiBold, 15f, 20f),
        // Counts, timestamps, filenames, toast body — drawn in thykra.textMeta.
        labelMedium = slot(text, FontWeight.Medium, 12.5f, 17.5f),
        // Micro-labels, badges, stamp eyebrow.
        labelSmall = slot(text, FontWeight.SemiBold, 11f, 13f, if (arabic) 0f else 0.08f),
    )

    val numeral = if (arabic) {
        slot(text, FontWeight.Bold, 40f * displayScale, 40f * displayScale, 0f, "tnum")
    } else {
        slot(fonts.display, FontWeight.Bold, 40f * displayScale, 40f * displayScale, -0.04f, "tnum")
    }

    return ThykraTypographySet(typography, numeral)
}

/**
 * Display slots and the numeral cap their scaling: a 44 sp title at 200% is 88 sp and no
 * longer a title. Body and label slots scale fully, as they must.
 */
const val DisplayFontScaleCap = 1.5f
