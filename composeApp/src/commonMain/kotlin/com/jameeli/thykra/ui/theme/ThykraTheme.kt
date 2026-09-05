package com.jameeli.thykra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp

/** Below this window width the display slots step down one. 360 dp is the floor. */
private val CompactWidthThreshold = 380.dp

/**
 * Wanderlust Editions, applied.
 *
 * @param mode which scheme to resolve. Defaults to the app-level [LocalThemeMode], which
 *   the Me screen's segmented control drives and [ThemePreference] persists.
 * @param forceDark the media viewer passes `true`: it is always Darkroom regardless of
 *   the preference, and it is the one place the two themes converge.
 */
@Composable
fun ThykraTheme(
    mode: ThemeMode = LocalThemeMode.current,
    forceDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = forceDark || when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Paper -> false
        ThemeMode.Darkroom -> true
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val compact = with(density) { windowInfo.containerSize.width.toDp() } < CompactWidthThreshold
    val arabic = Locale.current.language == "ar"

    val fonts = rememberThykraFonts()
    val typographySet = remember(fonts, compact, arabic, density.fontScale) {
        thykraTypography(
            fonts = fonts,
            compact = compact,
            arabic = arabic,
            fontScale = density.fontScale,
        )
    }

    CompositionLocalProvider(
        LocalExtendedColors provides if (dark) darkroomExtended else paperExtended,
        LocalElevation provides if (dark) darkroomElevation else paperElevation,
        LocalMotion provides thykraMotion,
        LocalReducedMotion provides platformReducedMotion(),
        LocalNumeralStyle provides typographySet.numeral,
        LocalArabic provides arabic,
        LocalCompactWidth provides compact,
    ) {
        MaterialTheme(
            colorScheme = if (dark) darkroomScheme else paperScheme,
            typography = typographySet.typography,
            shapes = thykraShapes,
            content = content,
        )
    }
}

/**
 * True when the app locale is Arabic. Read by the `Label` composable (which stops
 * uppercasing) and by the chapter numeral (which switches to Arabic-Indic digits).
 */
val LocalArabic = androidx.compose.runtime.staticCompositionLocalOf { false }

/** True below 380 dp of window width. Kit parts that drop a label read this. */
val LocalCompactWidth = androidx.compose.runtime.staticCompositionLocalOf { false }

val MaterialTheme.isArabic: Boolean
    @Composable get() = LocalArabic.current

val MaterialTheme.isCompactWidth: Boolean
    @Composable get() = LocalCompactWidth.current
