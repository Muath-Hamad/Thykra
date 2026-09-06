package com.jameeli.thykra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation is `Modifier.shadow(ambientColor = ink, spotColor = ink)` — that one call is
 * what "ink-tinted" means in Compose.
 *
 * In Darkroom, layers are surfaces and not shadows: every level is 0 dp and depth comes
 * from the container steps, except the top layer (sheet, dialog, toast, dock) which keeps
 * a 12 dp black shadow so it separates from the card it covers.
 */
@Immutable
data class ThykraElevation(
    /** Top bar, nav bar, action bar. */
    val level0: Dp,
    /** Resting card, chip. */
    val level1: Dp,
    /** Pressed card, dropdown. */
    val level2: Dp,
    /** Sheet, dialog, toast, dock. */
    val level3: Dp,
    /** Every photograph on a surface. */
    val plate: Dp,
    /** Darkroom draws its shadows in pure black at 70%; Paper tints them with ink. */
    val shadowColor: Color?,
    /** True when a hairline outline stands in for the shadow. */
    val hairlineInsteadOfShadow: Boolean,
)

val paperElevation = ThykraElevation(
    level0 = 0.dp,
    level1 = 1.dp,
    level2 = 3.dp,
    level3 = 8.dp,
    plate = 2.dp,
    shadowColor = null, // null = use the scheme's surfaceTint, which is ink in Paper
    hairlineInsteadOfShadow = false,
)

val darkroomElevation = ThykraElevation(
    level0 = 0.dp,
    level1 = 0.dp,
    level2 = 0.dp,
    level3 = 12.dp,
    plate = 0.dp,
    shadowColor = Color(0xFF000000).copy(alpha = 0.70f),
    hairlineInsteadOfShadow = true,
)

val LocalElevation = staticCompositionLocalOf { paperElevation }

val MaterialTheme.elevation: ThykraElevation
    @Composable @ReadOnlyComposable get() = LocalElevation.current

/**
 * The single shadow call in the app. Screens and kit parts use this and never
 * `Modifier.shadow` directly, so Darkroom's "no shadows" rule holds in one place.
 */
@Composable
fun Modifier.thykraShadow(elevation: Dp, shape: Shape): Modifier {
    if (elevation <= 0.dp) return this
    val tint = MaterialTheme.elevation.shadowColor ?: MaterialTheme.colorScheme.surfaceTint
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint,
        spotColor = tint,
    )
}
