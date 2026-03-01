package com.jameeli.thykra.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.auth.PlatformAppleSignInButton
import com.jameeli.thykra.auth.PlatformGoogleSignInButton
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.ThykraIcons

@Composable
fun LoginScreenContent(authViewModel: AuthViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branding section
            Text(
                text = "Thykra",
                style = MaterialTheme.typography.displayLarge,
                color = ThykraColors.SkyBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Travel Together. Remember Forever.",
                style = MaterialTheme.typography.bodyLarge,
                color = ThykraColors.MutedSlate,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sign-in card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThykraColors.Sandy
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = ThykraIcons.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = ThykraColors.SkyBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Secure Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThykraColors.DeepNavy
                    )
                    Spacer(modifier = Modifier.height(20.dp))

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
                }
            }
        }

        // Copyright footer
        Text(
            text = "\u00a9 2025 Thykra",
            style = MaterialTheme.typography.labelSmall,
            color = ThykraColors.MutedSlate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
