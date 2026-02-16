package com.jameeli.thykra.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun LoginScreenContent(authViewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Thykra",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Travel Together. Remember Forever.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        PlatformGoogleSignInButton(
            onIdToken = { idToken -> authViewModel.loginWithGoogle(idToken) },
            onError = { /* TODO: show error */ }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlatformAppleSignInButton(
            onIdToken = { idToken -> authViewModel.loginWithApple(idToken) },
            onError = { /* TODO: show error */ }
        )
    }
}
