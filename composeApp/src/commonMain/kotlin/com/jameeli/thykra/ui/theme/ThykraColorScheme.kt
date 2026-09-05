package com.jameeli.thykra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Wanderlust Editions colour, Paper and Darkroom.
 *
 * Every hex here is copied from the web's `tokens.css`, or derived from it by the two rules
 * in design part 1 §02: a Darkroom container is the accent at the token's alpha composited
 * over `#121114` and flattened to an opaque hex (so Coil placeholders and Glance can use it);
 * an on-container the web never named is the accent shifted two steps toward the text colour.
 *
 * Nothing outside `ui/theme/` and `ui/kit/` may name a colour that is not a role on
 * [ColorScheme] or a field on [ExtendedColors].
 */
internal object Paper {
    val accent = Color(0xFF1B6FBE)
    val onAccent = Color(0xFFFFFFFF)
    val accentSoft = Color(0xFFE4EFF9)
    val accentText = Color(0xFF124F8C)
    val accentHover = Color(0xFF155C9F)

    val warm = Color(0xFFAA4324)
    val onWarm = Color(0xFFFFFFFF)
    val warmSoft = Color(0xFFF7E7DF)
    val warmText = Color(0xFF8F3A1E)
    val warmHover = Color(0xFF8C3718)

    val bg = Color(0xFFFBF6ED)
    val bgRaised = Color(0xFFF4EDE0)
    val bgSunken = Color(0xFFEAE1D1)
    val bgLow = Color(0xFFF8F2E7) // derived: bg <-> raised midpoint

    val text = Color(0xFF151726)
    val text2 = Color(0xFF3B4453)
    val text3 = Color(0xFF5C6B7A)
    val bone = Color(0xFFF2EBDF)

    val rule = Color(0xFFE4E0D9) // ink @10% on bg, flattened
    val ruleStrong = Color(0xFFCDC9C5) // ink @20% on bg, flattened

    val bad = Color(0xFFC33A32)
    val onBad = Color(0xFFFFFFFF)
    val badSoft = Color(0xFFFBE7E4)
    val badText = Color(0xFFA32D26) // derived, to add to tokens.css

    val good = Color(0xFF1F7A4D)
    val goodSoft = Color(0xFFDFF0E6)
    val goodText = Color(0xFF146041) // derived, to add to tokens.css

    val warn = Color(0xFF8A5D12)
    val warnSoft = Color(0xFFF6EBD4)
    val warnText = Color(0xFF6B4708) // derived, to add to tokens.css
}

internal object Darkroom {
    val accent = Color(0xFF5AA6EA)
    val onAccent = Color(0xFF121114)
    val accentSoft = Color(0xFF1C2632) // accent @14% over ink
    val accentText = Color(0xFF8CC3F5)
    val accentHover = Color(0xFF7FBCF2)

    val warm = Color(0xFFE4794C)
    val onWarm = Color(0xFF121114)
    val warmSoft = Color(0xFF2F201C) // warm @14% over ink
    val warmText = Color(0xFFF0A183)
    val warmHover = Color(0xFFEE8F65)

    /** Not pure black — sits just above the OLED black-smear threshold. */
    val bg = Color(0xFF121114)
    val bgLow = Color(0xFF161418)
    val bgRaised = Color(0xFF1B191E)
    val bgHigh = Color(0xFF211F24)
    val bgSunken = Color(0xFF24212A)
    val bgDim = Color(0xFF0C0B0E)
    val bgBright = Color(0xFF2A272D)

    val text = Color(0xFFF2EBDF)
    val text2 = Color(0xFFCFC7BC)
    val text3 = Color(0xFFABA3A0)
    val ink = Color(0xFF151726)

    val rule = Color(0xFF2D2B2C) // bone @12%
    val ruleStrong = Color(0xFF484545) // bone @24%

    val bad = Color(0xFFF0736A)
    val onBad = Color(0xFF121114)
    val badSoft = Color(0xFF362122) // bad @16% over ink
    val badText = Color(0xFFF5A39C)

    val good = Color(0xFF4FBE85)
    val goodSoft = Color(0xFF1C2D26) // good @16% over ink
    val goodText = Color(0xFF8FD9B3)

    val warn = Color(0xFFE0AC4C)
    val warnSoft = Color(0xFF332A1D) // warn @16% over ink
    val warnText = Color(0xFFEDC680)
}

/**
 * Paper. Bone at full brightness is comfortable outdoors, which is where trips happen.
 * `surfaceTint` is ink so any tonal elevation Material applies on its own warms rather
 * than blues.
 */
val paperScheme: ColorScheme = lightColorScheme(
    primary = Paper.accent,
    onPrimary = Paper.onAccent,
    primaryContainer = Paper.accentSoft,
    onPrimaryContainer = Paper.accentText,
    inversePrimary = Darkroom.accent,

    secondary = Paper.text2,
    onSecondary = Paper.bg,
    secondaryContainer = Paper.bgSunken,
    onSecondaryContainer = Paper.text,

    tertiary = Paper.warm,
    onTertiary = Paper.onWarm,
    tertiaryContainer = Paper.warmSoft,
    onTertiaryContainer = Paper.warmText,

    background = Paper.bg,
    onBackground = Paper.text,
    surface = Paper.bg,
    onSurface = Paper.text,
    surfaceVariant = Paper.bgSunken,
    onSurfaceVariant = Paper.text2,
    surfaceDim = Paper.bgSunken,
    surfaceBright = Paper.bg,
    surfaceContainerLowest = Paper.bgSunken,
    surfaceContainerLow = Paper.bgLow,
    surfaceContainer = Paper.bgRaised,
    surfaceContainerHigh = Paper.bgRaised,
    surfaceContainerHighest = Paper.bgSunken,
    surfaceTint = Paper.text,

    inverseSurface = Paper.text,
    inverseOnSurface = Paper.bone,

    outline = Paper.ruleStrong,
    outlineVariant = Paper.rule,

    error = Paper.bad,
    onError = Paper.onBad,
    errorContainer = Paper.badSoft,
    onErrorContainer = Paper.badText,

    scrim = Paper.text,
)

/**
 * Darkroom. Layers are surfaces, not shadows: `surfaceTint` equals the surface so tonal
 * elevation is a no-op and depth comes from the container steps instead.
 */
val darkroomScheme: ColorScheme = darkColorScheme(
    primary = Darkroom.accent,
    onPrimary = Darkroom.onAccent,
    primaryContainer = Darkroom.accentSoft,
    onPrimaryContainer = Darkroom.accentText,
    inversePrimary = Paper.accent,

    secondary = Darkroom.text2,
    onSecondary = Darkroom.bg,
    secondaryContainer = Darkroom.bgSunken,
    onSecondaryContainer = Darkroom.text,

    tertiary = Darkroom.warm,
    onTertiary = Darkroom.onWarm,
    tertiaryContainer = Darkroom.warmSoft,
    onTertiaryContainer = Darkroom.warmText,

    background = Darkroom.bg,
    onBackground = Darkroom.text,
    surface = Darkroom.bg,
    onSurface = Darkroom.text,
    surfaceVariant = Darkroom.bgSunken,
    onSurfaceVariant = Darkroom.text2,
    surfaceDim = Darkroom.bgDim,
    surfaceBright = Darkroom.bgBright,
    surfaceContainerLowest = Darkroom.bgDim,
    surfaceContainerLow = Darkroom.bgLow,
    surfaceContainer = Darkroom.bgRaised,
    surfaceContainerHigh = Darkroom.bgHigh,
    surfaceContainerHighest = Darkroom.bgSunken,
    surfaceTint = Darkroom.bg,

    inverseSurface = Darkroom.text,
    inverseOnSurface = Darkroom.ink,

    outline = Darkroom.ruleStrong,
    outlineVariant = Darkroom.rule,

    error = Darkroom.bad,
    onError = Darkroom.onBad,
    errorContainer = Darkroom.badSoft,
    onErrorContainer = Darkroom.badText,

    scrim = Color(0xFF000000),
)
