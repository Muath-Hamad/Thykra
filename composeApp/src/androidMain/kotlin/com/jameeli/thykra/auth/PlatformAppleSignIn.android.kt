package com.jameeli.thykra.auth

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformAppleSignInButton(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    // Apple Sign-In is not available on Android
}
