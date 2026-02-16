package com.jameeli.thykra

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.navigation.AppNavHost

@Composable
fun App(authViewModel: AuthViewModel) {
    MaterialTheme {
        AppNavHost(authViewModel = authViewModel)
    }
}
