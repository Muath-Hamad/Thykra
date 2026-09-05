package com.jameeli.thykra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * What Material has no slot for. Reached as `MaterialTheme.thykra` — nothing in a screen
 * file names a hex, it names one of these.
 */
@Immutable
data class ExtendedColors(
    /** Upload row Done tick, toast leading bar for confirmations. */
    val success: Color,
    /** "You're already in" state, batch-complete dock row. */
    val successContainer: Color,
    val onSuccessContainer: Color,
    /** Expired invite stamp, "Confirming" upload label, offline banner icon. */
    val warning: Color,
    /** Offline banner fill. */
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** Counts, timestamps, filenames, uppercase labels. Meta only, never body. */
    val textMeta: Color,
    /** Filled button pressed container; Material's ripple stays on top. */
    val primaryPressed: Color,
    /** Join / Invite pressed. */
    val tertiaryPressed: Color,
    /** 1 dp border drawn over every plate. Alpha, because it sits on a photograph. */
    val plateOutline: Color,
    /** Destructive dialog scrim, viewer chrome pills, drop-target veil. */
    val scrimStrong: Color,
    /** Any text or icon over a photograph or scrim. Bone in both themes. */
    val onScrim: Color,
    /** Day numerals only, so they can be re-coloured without touching onSurface. */
    val numeral: Color,
    /** Initials-fallback fills, hashed by user id — same hash as web, so a person is
     *  the same colour on both platforms. Text on them is `onTertiary`. */
    val avatarTints: List<Color>,
) {
    /** `userId.hashCode() mod 5`, matching the web's `avatarTint()`. */
    fun avatarTint(userId: String): Color =
        avatarTints[((userId.hashCode() % avatarTints.size) + avatarTints.size) % avatarTints.size]
}

val paperExtended = ExtendedColors(
    success = Paper.good,
    successContainer = Paper.goodSoft,
    onSuccessContainer = Paper.goodText,
    warning = Paper.warn,
    warningContainer = Paper.warnSoft,
    onWarningContainer = Paper.warnText,
    textMeta = Paper.text3,
    primaryPressed = Paper.accentHover,
    tertiaryPressed = Paper.warmHover,
    plateOutline = Paper.text.copy(alpha = 0.10f),
    scrimStrong = Paper.text.copy(alpha = 0.72f),
    onScrim = Paper.bone,
    numeral = Paper.text,
    avatarTints = listOf(
        Color(0xFFAA4324),
        Color(0xFF1B6FBE),
        Color(0xFF4E5A69),
        Color(0xFF1F7A4D),
        Color(0xFF8A5D12),
    ),
)

val darkroomExtended = ExtendedColors(
    success = Darkroom.good,
    successContainer = Darkroom.goodSoft,
    onSuccessContainer = Darkroom.goodText,
    warning = Darkroom.warn,
    warningContainer = Darkroom.warnSoft,
    onWarningContainer = Darkroom.warnText,
    textMeta = Darkroom.text3,
    primaryPressed = Darkroom.accentHover,
    tertiaryPressed = Darkroom.warmHover,
    plateOutline = Darkroom.text.copy(alpha = 0.14f),
    scrimStrong = Color(0xFF000000).copy(alpha = 0.82f),
    onScrim = Darkroom.text,
    numeral = Darkroom.text,
    avatarTints = listOf(
        Color(0xFFE4794C),
        Color(0xFF5AA6EA),
        Color(0xFF8D98A6),
        Color(0xFF4FBE85),
        Color(0xFFE0AC4C),
    ),
)

val LocalExtendedColors = staticCompositionLocalOf { paperExtended }

/** `MaterialTheme.thykra.textMeta` and friends. */
val MaterialTheme.thykra: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

/** The scrim pill alpha the viewer chrome uses over a photograph. */
const val ScrimPillAlpha = 0.55f
