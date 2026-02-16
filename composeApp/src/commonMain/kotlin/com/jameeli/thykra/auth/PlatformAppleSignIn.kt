package com.jameeli.thykra.auth

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformAppleSignInButton(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
)
