package com.jameeli.thykra.ui.landing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.PlatformAppleSignInButton
import com.jameeli.thykra.auth.PlatformGoogleSignInButton
import com.jameeli.thykra.ui.theme.ThykraColors

// Curated travel images from the same pool as the web app
private val columnAImages = listOf(
    "https://images.unsplash.com/photo-1539635278303-d4002c07eae3?w=600&q=75",
    "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?w=600&q=75",
    "https://images.unsplash.com/photo-1517760444937-f6397edcbbcd?w=600&q=75",
    "https://images.unsplash.com/photo-1501555088652-021faa106b9b?w=600&q=75",
    "https://images.unsplash.com/photo-1489749798305-4fea3ae63d43?w=600&q=75",
    "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=600&q=75",
)

private val columnBImages = listOf(
    "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=600&q=75",
    "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=600&q=75",
    "https://images.unsplash.com/photo-1518684079-3c830dcef090?w=600&q=75",
    "https://images.unsplash.com/photo-1504150558240-0b4fd8946624?w=600&q=75",
    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=75",
    "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=600&q=75",
)

private val columnCImages = listOf(
    "https://images.unsplash.com/photo-1528543606781-2f6e6857f318?w=600&q=75",
    "https://images.unsplash.com/photo-1530521954074-e64f6810b32d?w=600&q=75",
    "https://images.unsplash.com/photo-1536940385103-c729049165e6?w=600&q=75",
    "https://images.unsplash.com/photo-1506929562872-bb421503ef21?w=600&q=75",
    "https://images.unsplash.com/photo-1454391304352-2bf4678b1a7a?w=600&q=75",
    "https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?w=600&q=75",
)

@Composable
fun LandingScreenContent(authViewModel: AuthViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated image background
        AnimatedImageColumns()

        // Gradient overlays for readability
        // Top gradient: warm white fading down
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to ThykraColors.WarmWhite,
                        0.3f to ThykraColors.WarmWhite.copy(alpha = 0.9f),
                        1f to Color.Transparent
                    )
                )
        )

        // Bottom gradient: warm white fading up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.3f to ThykraColors.WarmWhite.copy(alpha = 0.85f),
                        0.6f to ThykraColors.WarmWhite.copy(alpha = 0.95f),
                        1f to ThykraColors.WarmWhite
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top branding
            Spacer(modifier = Modifier.weight(0.12f))
            Text(
                text = "Thykra",
                style = MaterialTheme.typography.displayLarge,
                color = ThykraColors.SkyBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Travel Together.\nRemember Forever.",
                style = MaterialTheme.typography.bodyLarge,
                color = ThykraColors.MutedSlate,
                textAlign = TextAlign.Center
            )

            // Spacer to push sign-in to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Sign-in buttons at the bottom
            PlatformGoogleSignInButton(
                onIdToken = { idToken -> authViewModel.loginWithGoogle(idToken) },
                onError = { /* TODO: show error */ }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlatformAppleSignInButton(
                onIdToken = { idToken -> authViewModel.loginWithApple(idToken) },
                onError = { /* TODO: show error */ }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your password is never shared with Thykra",
                style = MaterialTheme.typography.labelSmall,
                color = ThykraColors.MutedSlate,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AnimatedImageColumns() {
    val infiniteTransition = rememberInfiniteTransition(label = "imageScroll")

    // Each column scrolls at a different speed and direction
    // Column height: 6 images * (imageHeight + gap). We scroll through one full set then loop.
    // Image height ~200dp, gap 8dp => total ~1248dp per set
    val totalScrollPx = 1248f

    val offsetA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -totalScrollPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "columnA"
    )

    val offsetB by infiniteTransition.animateFloat(
        initialValue = -totalScrollPx,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "columnB"
    )

    val offsetC by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -totalScrollPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "columnC"
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Three columns, each scrolling in different directions/speeds
        ImageColumn(
            images = columnAImages,
            scrollOffset = offsetA,
            modifier = Modifier.weight(1f)
        )
        ImageColumn(
            images = columnBImages,
            scrollOffset = offsetB,
            modifier = Modifier.weight(1f)
        )
        ImageColumn(
            images = columnCImages,
            scrollOffset = offsetC,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ImageColumn(
    images: List<String>,
    scrollOffset: Float,
    modifier: Modifier = Modifier
) {
    // Double the images for seamless looping
    val doubledImages = remember(images) { images + images }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, scrollOffset.toInt()) },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            doubledImages.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThykraColors.Sandy)
                )
            }
        }
    }
}
