package com.jameeli.thykra.ui.landing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.PlatformAppleSignInButton
import com.jameeli.thykra.auth.PlatformGoogleSignInButton
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.landing_plate_1
import com.jameeli.thykra.resources.landing_plate_2
import com.jameeli.thykra.resources.landing_plate_3
import com.jameeli.thykra.resources.landing_plate_4
import com.jameeli.thykra.resources.landing_plate_5
import com.jameeli.thykra.resources.landing_plate_6
import com.jameeli.thykra.resources.landing_plate_7
import com.jameeli.thykra.resources.landing_plate_8
import com.jameeli.thykra.ui.kit.Stamp
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ThemeMode
import com.jameeli.thykra.ui.theme.ThykraTheme
import com.jameeli.thykra.ui.theme.thykra
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Design part 3 §08. The unauthenticated entry.
 *
 * Paper only, by rule: it is a first impression, not a working surface, so it keeps its
 * fixed art direction whatever the system theme says.
 *
 * The wall is eight bundled JPEGs at 276 KB, not Unsplash at runtime. J8 says no blank
 * screens, and this is the one screen guaranteed to be shown to someone who has never
 * had a session — which means it has to render on a plane.
 */
@Composable
fun LandingScreenContent(authViewModel: AuthViewModel) {
    // Paper only, whatever the preference or the system says.
    ThykraTheme(mode = ThemeMode.Paper) {
        val scheme = MaterialTheme.colorScheme
        val extended = MaterialTheme.thykra

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surface),
        ) {
            PlateWall()

            // Bone from 15% down to opaque at 68%, so the type below always holds.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to scheme.surface.copy(alpha = 0.15f),
                            0.40f to scheme.surface.copy(alpha = 0.72f),
                            0.68f to scheme.surface,
                            1f to scheme.surface,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Stamp(eyebrow = "Thykra", name = "ذكرى")

                Spacer(Modifier.height(24.dp))

                Text(
                    text = clayPhrase("Travel together. ", "Remember forever."),
                    style = MaterialTheme.typography.displayLarge,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "One trip, everyone's photos, told by day.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = extended.textMeta,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                var signInError by remember { mutableStateOf<String?>(null) }

                SignInError(signInError)

                SignInButtons(authViewModel, onError = { signInError = it })

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "By continuing you agree to the Terms and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textMeta,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Google then Apple on Android; the platform decides the order, because Apple's own
 * guidance puts Sign in with Apple first on iOS and nowhere else.
 */
@Composable
private fun SignInButtons(authViewModel: AuthViewModel, onError: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlatformGoogleSignInButton(
            onIdToken = { token -> authViewModel.loginWithGoogle(token) },
            onError = onError,
        )
        PlatformAppleSignInButton(
            onIdToken = { token -> authViewModel.loginWithApple(token) },
            onError = onError,
        )
    }
}

/**
 * The same strip the web landing shows, for the same reason: this screen is outside the
 * shell, so it has no toast host, and a sign-in that fails silently leaves someone
 * tapping a button that appears to do nothing.
 */
@Composable
private fun SignInError(message: String?) {
    val scheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(scheme.errorContainer, MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ThykraIcons.Alert,
                contentDescription = null,
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onErrorContainer,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private val WallPlates: List<DrawableResource> = listOf(
    Res.drawable.landing_plate_1,
    Res.drawable.landing_plate_2,
    Res.drawable.landing_plate_3,
    Res.drawable.landing_plate_4,
    Res.drawable.landing_plate_5,
    Res.drawable.landing_plate_6,
    Res.drawable.landing_plate_7,
    Res.drawable.landing_plate_8,
)

/** 12 dp/s, alternating direction. Still under reduced motion. */
private const val DriftDpPerSecond = 12f

@Composable
private fun PlateWall() {
    val reduced = LocalReducedMotion.current
    val density = LocalDensity.current

    // Each column shows a rotation of the set, so the three never line up.
    val columns = remember {
        listOf(
            WallPlates,
            WallPlates.drop(3) + WallPlates.take(3),
            WallPlates.drop(5) + WallPlates.take(5),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            // The wall decorates a screen that already names the product.
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        columns.forEachIndexed { index, plates ->
            PlateColumn(
                plates = plates,
                // Alternating direction, and a slightly different speed per column so the
                // wall never reads as one sheet sliding.
                upwards = index % 2 == 0,
                speedScale = 1f + index * 0.15f,
                reduced = reduced,
                densityScale = density.density,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlateColumn(
    plates: List<DrawableResource>,
    upwards: Boolean,
    speedScale: Float,
    reduced: Boolean,
    densityScale: Float,
    modifier: Modifier = Modifier,
) {
    // One plate is 4:3 at a third of the width; doubling the list makes the loop seamless.
    val doubled = remember(plates) { plates + plates }
    val plateHeightDp = 150f
    val gapDp = 6f
    val setHeightDp = plates.size * (plateHeightDp + gapDp)
    val durationMs = ((setHeightDp / (DriftDpPerSecond * speedScale)) * 1000).toInt()

    val transition = rememberInfiniteTransition(label = "plateWall")
    val progress by transition.animateFloat(
        initialValue = if (upwards) 0f else 1f,
        targetValue = if (upwards) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "plateDrift",
    )

    val offsetPx = if (reduced) 0f else -progress * setHeightDp * densityScale

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.offset { IntOffset(0, offsetPx.toInt()) },
            verticalArrangement = Arrangement.spacedBy(gapDp.dp),
        ) {
            doubled.forEach { plate ->
                Image(
                    painter = painterResource(plate),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, PlateShape),
                )
            }
        }
    }
}
