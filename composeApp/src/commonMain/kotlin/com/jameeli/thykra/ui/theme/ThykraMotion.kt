package com.jameeli.thykra.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Four durations, three eases. Anything not in the motion table of design part 4 §14
 * does not animate.
 */
@Immutable
data class ThykraMotion(
    val dur1: Int = 120,
    val dur2: Int = 200,
    val dur3: Int = 320,
    val dur4: Int = 520,
    val easeOut: Easing = CubicBezierEasing(0.2f, 0.8f, 0.28f, 1f),
    val easeInOut: Easing = CubicBezierEasing(0.5f, 0f, 0.3f, 1f),
    /**
     * Reaction burst only. Not a Compose `spring()`: the overshoot is authored, not
     * physical, so it matches the web frame for frame.
     */
    val spring: Easing = CubicBezierEasing(0.2f, 1.25f, 0.3f, 1f),
    val staggerMs: Int = 40,
    /** A 40-plate chapter must never queue 1.6 s of entrances. */
    val staggerCap: Int = 8,
) {
    /** Delay for the item at [index] in a staggered entrance. */
    fun staggerDelay(index: Int): Int = minOf(index, staggerCap) * staggerMs
}

val thykraMotion = ThykraMotion()

val LocalMotion = staticCompositionLocalOf { thykraMotion }

/**
 * True when the platform asks for reduced motion (Android `ANIMATOR_DURATION_SCALE == 0`,
 * iOS `isReduceMotionEnabled`). Read once at the theme root.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

val MaterialTheme.motion: ThykraMotion
    @Composable @ReadOnlyComposable get() = LocalMotion.current

val MaterialTheme.reducedMotion: Boolean
    @Composable @ReadOnlyComposable get() = LocalReducedMotion.current

/**
 * The one tween factory. Screens never branch on the reduced-motion flag themselves —
 * they ask for the duration and ease they want and get a `dur1` fade instead when the
 * user has asked for less movement.
 *
 * Pass `transform = true` for anything whose target is an offset, scale or rotation:
 * those collapse to a [ThykraMotion.dur1] cross-fade under reduced motion. Colour and
 * alpha tweens keep their own duration, shortened to `dur1`.
 */
@Composable
fun <T> thykraTween(
    durationMillis: Int = LocalMotion.current.dur2,
    easing: Easing = LocalMotion.current.easeOut,
    delayMillis: Int = 0,
): TweenSpec<T> {
    val motion = LocalMotion.current
    return if (LocalReducedMotion.current) {
        tween(durationMillis = motion.dur1, easing = motion.easeOut)
    } else {
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
    }
}

/** Stagger delay that collapses to zero under reduced motion. */
@Composable
fun staggerDelay(index: Int): Int =
    if (LocalReducedMotion.current) 0 else LocalMotion.current.staggerDelay(index)

/**
 * The screen-content entrance: fade in and rise 12 dp, staggered 40 ms for the first
 * eight items. Under reduced motion the rise and the stagger both go to zero and only
 * the fade is left, which is the whole rule of part 4 §14 in one modifier.
 */
@Composable
fun Modifier.thykraAnimate(index: Int = 0, visible: Boolean = true): Modifier {
    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current
    val delay = if (reduced) 0 else motion.staggerDelay(index)
    val duration = if (reduced) motion.dur1 else motion.dur3
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(duration, delay, motion.easeOut),
        label = "thykraAnimateAlpha",
    )
    val rise by animateDpAsState(
        targetValue = if (visible || reduced) 0.dp else 12.dp,
        animationSpec = tween(duration, delay, motion.easeOut),
        label = "thykraAnimateRise",
    )
    return this
        .graphicsLayer {
            this.alpha = alpha
            translationY = rise.toPx()
        }
}
