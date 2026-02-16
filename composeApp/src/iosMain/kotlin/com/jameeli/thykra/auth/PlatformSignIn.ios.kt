package com.jameeli.thykra.auth

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformGoogleSignInButton(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    Button(onClick = {
        // iOS Google Sign-In will be implemented in Batch 6
        onError("Google Sign-In not yet implemented on iOS")
    }) {
        Text("Sign in with Google")
    }
}
