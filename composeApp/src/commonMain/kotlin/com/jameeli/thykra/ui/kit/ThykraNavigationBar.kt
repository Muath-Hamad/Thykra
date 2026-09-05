package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.ThykraIcons

/** The four roots. Nothing else lives on the bar. */
enum class RootTab(val label: String) {
    Trips("Trips"),
    Activity("Activity"),
    Recaps("Recaps"),
    Me("Me"),
}

@Immutable
data class NavBarState(
    val selected: RootTab,
    /** A dot, never a count: people did something, and the number does not help. */
    val activityDot: Boolean = false,
    /** The Me tab shows the signed-in person rather than a glyph, when we have one. */
    val meAvatar: AvatarUser? = null,
)

/**
 * Part 2 §4.11.
 *
 * 60 dp plus the navigation inset, `surface`, a top hairline and no tonal elevation. It
 * shows only on the four roots: inside a trip it gives its height to [TripActionBar], and
 * back restores it.
 */
@Composable
fun ThykraNavigationBar(
    state: NavBarState,
    onSelect: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = scheme.outlineVariant)
        NavigationBar(
            containerColor = scheme.surface,
            contentColor = scheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                // The bar never moves with the keyboard.
                .navigationBarsPadding(),
        ) {
            RootTab.entries.forEach { tab ->
                val selected = tab == state.selected
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    icon = { TabIcon(tab, selected, state) },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.primary,
                        selectedTextColor = scheme.onSurface,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                        indicatorColor = scheme.primaryContainer,
                    ),
                    modifier = Modifier.semantics {
                        if (tab == RootTab.Activity && state.activityDot) {
                            stateDescription = "new"
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TabIcon(tab: RootTab, selected: Boolean, state: NavBarState) {
    val scheme = MaterialTheme.colorScheme

    Box {
        when (tab) {
            RootTab.Me -> {
                val avatar = state.meAvatar
                if (avatar != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (selected) {
                                    // A ring rather than a fill, so a real face still reads.
                                    Modifier.border(2.dp, scheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Avatar(user = avatar, size = AvatarSize.Xs)
                    }
                } else {
                    Icon(
                        imageVector = ThykraIcons.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            else -> Icon(
                imageVector = tab.glyph(selected),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        if (tab == RootTab.Activity && state.activityDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(8.dp)
                    .background(scheme.tertiary, CircleShape),
            )
        }
    }
}

private fun RootTab.glyph(selected: Boolean): ImageVector = when (this) {
    RootTab.Trips -> if (selected) ThykraIcons.TripsFilled else ThykraIcons.Trips
    RootTab.Activity -> if (selected) ThykraIcons.ActivityFilled else ThykraIcons.Activity
    RootTab.Recaps -> if (selected) ThykraIcons.RecapsFilled else ThykraIcons.Recaps
    RootTab.Me -> ThykraIcons.Person
}
