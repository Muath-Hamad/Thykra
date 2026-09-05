package com.jameeli.thykra.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.kit.NavBarState
import com.jameeli.thykra.ui.kit.RootTab
import com.jameeli.thykra.ui.kit.ThykraNavigationBar
import com.jameeli.thykra.ui.kit.ToastHost
import com.jameeli.thykra.ui.kit.ToastState
import com.jameeli.thykra.ui.kit.AvatarUser
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * The chrome a screen can ask the shell to draw for it.
 *
 * The nav bar, the trip action bar, the upload dock and the toast are all hosted by the
 * Scaffold rather than by whatever screen happens to be on top. That is what lets the
 * dock survive navigation and re-mount from the persisted queue on a cold start, and it
 * is why a screen *asks* for a bottom bar instead of drawing one.
 */
@Stable
class ThykraChrome {

    /** Replaces the nav bar while a nested screen owns the bottom edge. */
    var bottomBar by mutableStateOf<(@Composable () -> Unit)?>(null)
        internal set

    /** Rides 8 dp above whichever bar is showing. Filled in build step 06. */
    var uploadDock by mutableStateOf<(@Composable () -> Unit)?>(null)
        internal set

    /** One at a time; a new one replaces. */
    val toast = ToastState()

    /** The signed-in person, for the Me tab's avatar. */
    var currentUser by mutableStateOf<AvatarUser?>(null)

    /** A dot, never a count. */
    var activityDot by mutableStateOf(false)
}

val LocalThykraChrome = staticCompositionLocalOf { ThykraChrome() }

/**
 * Hands the shell a bottom bar for as long as this screen is composed, and takes it back
 * on the way out — so back always restores the nav bar without the screen having to
 * remember to.
 */
@Composable
fun ProvideBottomBar(content: @Composable () -> Unit) {
    val chrome = LocalThykraChrome.current
    DisposableEffect(chrome) {
        chrome.bottomBar = content
        onDispose { chrome.bottomBar = null }
    }
}

/** Same contract, for the upload dock. */
@Composable
fun ProvideUploadDock(content: (@Composable () -> Unit)?) {
    val chrome = LocalThykraChrome.current
    DisposableEffect(chrome, content) {
        chrome.uploadDock = content
        onDispose { chrome.uploadDock = null }
    }
}

/**
 * The app shell: content, then whatever chrome owns the bottom edge, then the dock above
 * it, then the toast above that.
 *
 * @param selectedTab which root the user is on, or null on a nested screen — which is
 *   also the signal that the nav bar should give way.
 */
@Composable
fun ThykraShell(
    chrome: ThykraChrome,
    selectedTab: RootTab?,
    onSelectTab: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
    /** True on the routes that live outside the shell entirely — landing, invite, viewer. */
    fullBleed: Boolean = false,
    content: @Composable (innerPadding: PaddingValues) -> Unit,
) {
    val motion = LocalMotion.current
    val bottomBar = chrome.bottomBar
    val dock = chrome.uploadDock

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            if (fullBleed) return@Scaffold
            Column(modifier = Modifier.fillMaxWidth()) {
                // The dock rides above the bar, whichever bar that is.
                AnimatedVisibility(
                    visible = dock != null,
                    enter = fadeIn(thykraTween(motion.dur3)) +
                        slideInVertically(thykraTween(motion.dur3)) { it },
                    exit = fadeOut(thykraTween(motion.dur2)) +
                        slideOutVertically(thykraTween(motion.dur2)) { it },
                ) {
                    Column {
                        dock?.invoke()
                        Spacer(Modifier.padding(4.dp))
                    }
                }

                // A nested screen's own bar wins; otherwise the four tabs, on a root only.
                when {
                    bottomBar != null -> bottomBar.invoke()
                    selectedTab != null -> ThykraNavigationBar(
                        state = NavBarState(
                            selected = selectedTab,
                            activityDot = chrome.activityDot,
                            meAvatar = chrome.currentUser,
                        ),
                        onSelect = onSelectTab,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            content(innerPadding)
            // 12 dp above the bottom chrome, which the Scaffold already measured for us.
            ToastHost(
                state = chrome.toast,
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomPadding = innerPadding.calculateBottomPadding() + 12.dp,
            )
        }
    }
}
