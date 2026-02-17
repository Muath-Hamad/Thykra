package com.jameeli.thykra.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.auth.AuthState
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.ui.albums.AlbumDetailScreenContent
import com.jameeli.thykra.ui.albums.AlbumDetailViewModel
import com.jameeli.thykra.ui.albums.AlbumListScreenContent
import com.jameeli.thykra.ui.albums.AlbumListViewModel
import com.jameeli.thykra.ui.login.LoginScreenContent
import com.jameeli.thykra.ui.profile.ProfileScreenContent

@Composable
fun AppNavHost(authViewModel: AuthViewModel, albumApi: AlbumApi) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> navController.navigate(LoginScreen) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Authenticated -> navController.navigate(AlbumListScreen) {
                popUpTo(0) { inclusive = true }
            }
            is AuthState.Loading -> {}
        }
    }

    NavHost(navController = navController, startDestination = LoginScreen) {
        composable<LoginScreen> {
            LoginScreenContent(authViewModel = authViewModel)
        }
        composable<AlbumListScreen> {
            AlbumListScreenContent(
                viewModel = AlbumListViewModel(albumApi),
                onNavigateToAlbum = { albumId ->
                    navController.navigate(AlbumDetailScreen(albumId))
                },
                onNavigateToProfile = { navController.navigate(ProfileScreen) }
            )
        }
        composable<AlbumDetailScreen> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumDetailScreen>()
            AlbumDetailScreenContent(
                albumId = route.albumId,
                viewModel = AlbumDetailViewModel(albumApi),
                onNavigateBack = { navController.popBackStack() }
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
