package com.jameeli.thykra.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jameeli.thykra.auth.AuthState
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.ui.home.HomeScreenContent
import com.jameeli.thykra.ui.login.LoginScreenContent
import com.jameeli.thykra.ui.profile.ProfileScreenContent

@Composable
fun AppNavHost(authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> navController.navigate(LoginScreen) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Authenticated -> navController.navigate(HomeScreen) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Loading -> {}
        }
    }

    NavHost(navController = navController, startDestination = LoginScreen) {
        composable<LoginScreen> {
            LoginScreenContent(authViewModel = authViewModel)
        }
        composable<HomeScreen> {
            HomeScreenContent(
                onNavigateToProfile = { navController.navigate(ProfileScreen) }
            )
        }
        composable<ProfileScreen> {
            ProfileScreenContent(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
