package com.jameeli.thykra.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jameeli.thykra.KitGalleryEnabled
import com.jameeli.thykra.api.ActivityFeedApi
import com.jameeli.thykra.api.AlbumApi
import com.jameeli.thykra.api.RecapApi
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.CommentApi
import com.jameeli.thykra.api.InviteApi
import com.jameeli.thykra.api.MediaApi
import com.jameeli.thykra.api.ProfileApi
import com.jameeli.thykra.api.ReactionApi
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.auth.AuthState
import com.jameeli.thykra.auth.AuthViewModel
import com.jameeli.thykra.ui.kit.RootTab
import com.jameeli.thykra.ui.kit.gallery.KitGalleryScreen
import com.jameeli.thykra.ui.activity.ActivityScreen
import com.jameeli.thykra.ui.activity.ActivityViewModel
import com.jameeli.thykra.ui.recaps.RecapReaderScreen
import com.jameeli.thykra.ui.recaps.RecapReaderViewModel
import com.jameeli.thykra.ui.recaps.RecapsScreen
import com.jameeli.thykra.ui.recaps.RecapsViewModel
import com.jameeli.thykra.ui.invite.InviteScreen
import com.jameeli.thykra.ui.invite.InviteViewModel
import com.jameeli.thykra.ui.landing.LandingScreenContent
import com.jameeli.thykra.ui.trip.TripScreen
import com.jameeli.thykra.ui.trip.TripViewModel
import com.jameeli.thykra.ui.trips.TripsScreen
import com.jameeli.thykra.ui.trips.TripsViewModel
import com.jameeli.thykra.ui.media.MediaViewerScreenContent
import com.jameeli.thykra.ui.settings.TripSettingsScreen
import com.jameeli.thykra.ui.settings.TripSettingsViewModel
import com.jameeli.thykra.ui.media.MediaViewerViewModel
import com.jameeli.thykra.ui.profile.ProfileScreenContent
import com.jameeli.thykra.ui.social.MediaCommentsViewModel
import com.jameeli.thykra.ui.social.MediaReactionsViewModel
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.LocalReducedMotion
import com.jameeli.thykra.ui.theme.ThykraMotion
import kotlinx.serialization.serializer
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The route graph and the shell it mounts inside.
 *
 * Build step 03. Screens still waiting on their own step mount here unchanged — the shell
 * is what changes, not yet what it holds.
 */
@Composable
fun AppNavHost(
    authViewModel: AuthViewModel,
    albumApi: AlbumApi,
    mediaApi: MediaApi,
    reactionApi: ReactionApi,
    commentApi: CommentApi,
    profileApi: ProfileApi,
    uploadQueueManager: UploadQueueManager,
    inviteApi: InviteApi,
    activityFeedApi: ActivityFeedApi,
    recapApi: RecapApi,
    networkMonitor: NetworkMonitor? = null,
) {
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()
    val chrome = remember { ThykraChrome() }

    val motion = LocalMotion.current
    val reduced = LocalReducedMotion.current

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedTab = destination?.rootTab()
    val signedOut = authState is AuthState.Unauthenticated
    // Landing, invite and the viewer own the whole screen; nothing else does.
    val fullBleed = destination == null ||
        destination.isRoute<Landing>() ||
        destination.isRoute<Invite>() ||
        destination.isRoute<Viewer>() ||
        destination.isRoute<RecapReader>() ||
        destination.isRoute<KitGallery>()

    AuthRouting(authState, navController)
    DeepLinkRouting(authState, navController)

    CompositionLocalProvider(LocalThykraChrome provides chrome) {
        ThykraShell(
            chrome = chrome,
            selectedTab = if (signedOut) null else selectedTab,
            onSelectTab = { tab -> navController.switchTab(tab) },
            fullBleed = fullBleed,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Landing,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { pushEnter(motion, reduced) },
                exitTransition = { pushExit(motion, reduced) },
                popEnterTransition = { popEnter(motion, reduced) },
                popExitTransition = { popExit(motion, reduced) },
            ) {
                composable<Landing> {
                    LandingScreenContent(authViewModel = authViewModel)
                }

                // ── Roots. Tab switches cross-fade rather than slide. ──────────
                composable<Trips>(
                    enterTransition = { tabEnter(motion, reduced) },
                    exitTransition = { tabExit(motion, reduced) },
                ) {
                    val viewModel = remember {
                        TripsViewModel(albumApi, profileApi, networkMonitor)
                    }
                    TripsScreen(
                        viewModel = viewModel,
                        onOpenTrip = { albumId -> navController.navigate(Trip(albumId)) },
                        uploadQueueManager = uploadQueueManager,
                        networkMonitor = networkMonitor,
                    )
                }

                composable<ActivityFeed>(
                    enterTransition = { tabEnter(motion, reduced) },
                    exitTransition = { tabExit(motion, reduced) },
                ) {
                    val viewModel = remember { ActivityViewModel(activityFeedApi) }
                    ActivityScreen(
                        viewModel = viewModel,
                        onOpenMedia = { album, mediaId ->
                            navController.navigate(Viewer(album, mediaId))
                        },
                        onOpenTrip = { albumId -> navController.navigate(Trip(albumId)) },
                    )
                }

                composable<RecapsList>(
                    enterTransition = { tabEnter(motion, reduced) },
                    exitTransition = { tabExit(motion, reduced) },
                ) {
                    val viewModel = remember { RecapsViewModel(recapApi, albumApi) }
                    RecapsScreen(
                        viewModel = viewModel,
                        onOpenRecap = { token -> navController.navigate(RecapReader(token)) },
                    )
                }

                composable<Me>(
                    enterTransition = { tabEnter(motion, reduced) },
                    exitTransition = { tabExit(motion, reduced) },
                ) {
                    // Build step 11 replaces this with the Me screen.
                    ProfileScreenContent(
                        authViewModel = authViewModel,
                        onNavigateBack = { navController.switchTab(RootTab.Trips) },
                    )
                }

                // ── Nested. ───────────────────────────────────────────────────
                composable<Trip> { entry ->
                    val route = entry.toRoute<Trip>()
                    val viewModel = remember(route.albumId) {
                        TripViewModel(
                            albumApi = albumApi,
                            mediaApi = mediaApi,
                            profileApi = profileApi,
                            uploadQueueManager = uploadQueueManager,
                            networkMonitor = networkMonitor,
                        )
                    }
                    TripScreen(
                        albumId = route.albumId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenViewer = { mediaId ->
                            navController.navigate(Viewer(route.albumId, mediaId))
                        },
                        onOpenSettings = { navController.navigate(TripSettings(route.albumId)) },
                        onOpenActivity = { navController.navigate(TripActivity(route.albumId)) },
                        onOpenRecaps = { navController.navigate(TripRecaps(route.albumId)) },
                    )
                }

                composable<TripSettings> { entry ->
                    val route = entry.toRoute<TripSettings>()
                    val viewModel = remember(route.albumId) {
                        TripSettingsViewModel(albumApi, profileApi, networkMonitor)
                    }
                    TripSettingsScreen(
                        albumId = route.albumId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onLeftOrDeleted = {
                            navController.navigate(Trips) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }

                composable<TripActivity> { entry ->
                    val route = entry.toRoute<TripActivity>()
                    val viewModel = remember(route.albumId) {
                        ActivityViewModel(activityFeedApi, albumId = route.albumId)
                    }
                    ActivityScreen(
                        viewModel = viewModel,
                        onOpenMedia = { album, mediaId ->
                            navController.navigate(Viewer(album, mediaId))
                        },
                        onOpenTrip = { albumId -> navController.navigate(Trip(albumId)) },
                        tripTitle = "Trip activity",
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<TripRecaps> { entry ->
                    val route = entry.toRoute<TripRecaps>()
                    val viewModel = remember(route.albumId) { RecapsViewModel(recapApi, albumApi) }
                    RecapsScreen(
                        viewModel = viewModel,
                        onOpenRecap = { token -> navController.navigate(RecapReader(token)) },
                        albumId = route.albumId,
                    )
                }

                composable<Viewer> { entry ->
                    val route = entry.toRoute<Viewer>()
                    val viewModel = remember(route.albumId, route.mediaId) {
                        MediaViewerViewModel(mediaApi, albumApi, profileApi)
                    }
                    MediaViewerScreenContent(
                        albumId = route.albumId,
                        initialMediaId = route.mediaId,
                        viewModel = viewModel,
                        reactionsViewModelFactory = { MediaReactionsViewModel(reactionApi) },
                        commentsViewModelFactory = { MediaCommentsViewModel(commentApi) },
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

                composable<RecapReader> { entry ->
                    val route = entry.toRoute<RecapReader>()
                    val viewModel = remember(route.recapId) { RecapReaderViewModel(recapApi) }
                    RecapReaderScreen(
                        shareToken = route.recapId,
                        viewModel = viewModel,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable<Invite> { entry ->
                    val route = entry.toRoute<Invite>()
                    val viewModel = remember(route.token) { InviteViewModel(inviteApi) }
                    InviteScreen(
                        token = route.token,
                        viewModel = viewModel,
                        signedIn = authState is AuthState.Authenticated,
                        onJoined = { albumId ->
                            navController.navigate(Trip(albumId)) {
                                popUpTo(Trips) { inclusive = false }
                            }
                        },
                        // The token stays in DeepLinkBus.pending, so sign-in lands back here.
                        onSignIn = { navController.navigate(Landing) },
                        onClose = {
                            if (authState is AuthState.Authenticated) {
                                navController.navigate(Trips) { popUpTo(0) { inclusive = true } }
                            } else {
                                navController.navigate(Landing) { popUpTo(0) { inclusive = true } }
                            }
                        },
                        onOpenTrip = { albumId -> navController.navigate(Trip(albumId)) },
                    )
                }

                if (KitGalleryEnabled) {
                    composable<KitGallery> {
                        KitGalleryScreen(onClose = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

/**
 * Sign-in and sign-out both rebuild the graph from the root.
 *
 * A signed-out person is sent to Landing unless they arrived on a link that does not need
 * a session — an invite or a public recap — in which case they land where the link
 * pointed, and the link is held for after they sign in.
 */
@Composable
private fun AuthRouting(authState: AuthState, navController: NavHostController) {
    val pending by DeepLinkBus.pending.collectAsState()

    LaunchedEffect(authState, pending) {
        when (authState) {
            is AuthState.Loading -> Unit

            is AuthState.Unauthenticated -> {
                val target = pending
                if (target != null && target.reachableSignedOut) {
                    navController.navigateToTarget(target, clearStack = true)
                } else {
                    navController.navigate(Landing) { popUpTo(0) { inclusive = true } }
                }
            }

            is AuthState.Authenticated -> {
                val target = pending
                navController.navigate(Trips) { popUpTo(0) { inclusive = true } }
                if (target != null) {
                    navController.navigateToTarget(target, clearStack = false)
                    DeepLinkBus.consume(target)
                }
            }
        }
    }
}

/** Links that arrive while the app is already running. */
@Composable
private fun DeepLinkRouting(authState: AuthState, navController: NavHostController) {
    LaunchedEffect(authState) {
        DeepLinkBus.events.collect { target ->
            val signedIn = authState is AuthState.Authenticated
            if (!signedIn && !target.reachableSignedOut) {
                // Held in `pending` — AuthRouting picks it up after sign-in.
                return@collect
            }
            navController.navigateToTarget(target, clearStack = false)
            DeepLinkBus.consume(target)
        }
    }
}

private fun NavController.navigateToTarget(target: DeepLinkTarget, clearStack: Boolean) {
    val options: (NavOptionsBuilder.() -> Unit) = {
        if (clearStack) popUpTo(0) { inclusive = true }
    }
    when (target) {
        is DeepLinkTarget.TripList -> navigate(Trips, options)
        is DeepLinkTarget.Trip -> navigate(Trip(target.albumId), options)
        is DeepLinkTarget.Media -> {
            navigate(Trip(target.albumId), options)
            navigate(Viewer(target.albumId, target.mediaId))
        }
        is DeepLinkTarget.Invite -> navigate(Invite(target.token), options)
        is DeepLinkTarget.Recap -> navigate(RecapReader(target.shareToken), options)
    }
}

/**
 * Tabs are siblings, not a stack: switching pops back to the root rather than piling
 * roots on top of each other, and each tab keeps its own scroll position.
 */
private fun NavController.switchTab(tab: RootTab) {
    val route: Any = when (tab) {
        RootTab.Trips -> Trips
        RootTab.Activity -> ActivityFeed
        RootTab.Recaps -> RecapsList
        RootTab.Me -> Me
    }
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination.rootTab(): RootTab? = when {
    isRoute<Trips>() -> RootTab.Trips
    isRoute<ActivityFeed>() -> RootTab.Activity
    isRoute<RecapsList>() -> RootTab.Recaps
    isRoute<Me>() -> RootTab.Me
    else -> null
}

/**
 * "Is this destination route T?"
 *
 * The reified `hasRoute<T>()` overload is not in the multiplatform navigation artifact at
 * this version, so this compares serial names instead. Deriving the name from the
 * serializer rather than writing it out keeps a route rename from silently breaking the
 * nav bar: a class that no longer exists will not compile here.
 */
private inline fun <reified T : Any> NavDestination.isRoute(): Boolean {
    val route = this.route ?: return false
    // Data-class routes append their arguments: "…navigation.Trip/{albumId}".
    return route.substringBefore('/') == serializer<T>().descriptor.serialName
}

// ── Transitions, from design part 4 §14 ───────────────────────────────────────

/**
 * Push: the incoming screen slides in from the end and fades; the outgoing one fades to
 * 60% and scales to .96, so it reads as going *under* rather than away.
 *
 * These are plain functions rather than composables because a NavHost transition lambda
 * runs outside composition — the motion tokens are read once at the shell and passed in.
 *
 * Under reduced motion every one collapses to a 120 ms cross-fade, which is the rule the
 * whole app follows, applied here rather than branched on per screen.
 */
private fun pushEnter(motion: ThykraMotion, reduced: Boolean): EnterTransition =
    if (reduced) {
        fadeIn(tween(motion.dur1))
    } else {
        slideInHorizontally(tween(motion.dur3, easing = motion.easeOut)) { it / 12 } +
            fadeIn(tween(motion.dur3))
    }

private fun pushExit(motion: ThykraMotion, reduced: Boolean): ExitTransition =
    if (reduced) {
        fadeOut(tween(motion.dur1))
    } else {
        fadeOut(tween(motion.dur3), targetAlpha = 0.6f) +
            scaleOut(tween(motion.dur3), targetScale = 0.96f)
    }

private fun popEnter(motion: ThykraMotion, reduced: Boolean): EnterTransition =
    if (reduced) {
        fadeIn(tween(motion.dur1))
    } else {
        fadeIn(tween(motion.dur2), initialAlpha = 0.6f) +
            scaleIn(tween(motion.dur2), initialScale = 0.96f)
    }

private fun popExit(motion: ThykraMotion, reduced: Boolean): ExitTransition =
    if (reduced) {
        fadeOut(tween(motion.dur1))
    } else {
        slideOutHorizontally(tween(motion.dur2, easing = motion.easeOut)) { it / 12 } +
            fadeOut(tween(motion.dur2))
    }

/** Tab switch: content cross-fades. Nothing slides — the tabs are siblings. */
private fun tabEnter(motion: ThykraMotion, reduced: Boolean): EnterTransition =
    fadeIn(tween(if (reduced) motion.dur1 else motion.dur2))

private fun tabExit(motion: ThykraMotion, reduced: Boolean): ExitTransition =
    fadeOut(tween(if (reduced) motion.dur1 else motion.dur2))
