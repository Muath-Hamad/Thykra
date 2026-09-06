package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.nav_activity
import com.jameeli.thykra.resources.nav_activity_unseen
import com.jameeli.thykra.resources.nav_recaps
import com.jameeli.thykra.resources.nav_trips
import com.jameeli.thykra.resources.nav_you
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.ui.theme.DisplayFontScaleCap
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykraTween

/** The four roots. Nothing else lives on the bar. */
enum class RootTab(val label: StringResource) {
    Trips(Res.string.nav_trips),
    Activity(Res.string.nav_activity),
    Recaps(Res.string.nav_recaps),
    Me(Res.string.nav_you),
}

@Immutable
data class NavBarState(
    val selected: RootTab,
    /** A dot, never a count: people did something, and the number does not help. */
    val activityDot: Boolean = false,
    /** The Me tab shows the signed-in person rather than a glyph, when we have one. */
    val meAvatar: AvatarUser? = null,
)

private val IconSize = 24.dp
private val IndicatorWidth = 56.dp
private val IndicatorHeight = 28.dp
private val IconLabelGap = 4.dp
private val BarVerticalPadding = 6.dp

/** The floor from the spec. The bar only ever grows past it, never shrinks below. */
private val MinBarHeight = 60.dp

/**
 * Past this the tabs stop spreading and centre instead. Four items strung across a
 * tablet put the last one under nobody's thumb, and the design's answer for a real
 * tablet is a NavigationRail at the start edge rather than a wider bar.
 */
private val BarMaxWidth = 640.dp

/**
 * Part 2 §4.11.
 *
 * 60 dp *plus* the navigation inset, `surface`, a top hairline and no tonal elevation. It
 * shows only on the four roots: inside a trip it gives its height to [TripActionBar], and
 * back restores it.
 *
 * The items are laid out here rather than with `NavigationBarItem` because M3's item is
 * built for an 80 dp bar — a 32 dp indicator inside 16 dp of vertical padding — and
 * squeezing that into 60 dp overflows its own layout and clips the label. Owning the row
 * costs a few lines and buys the spec's exact 56 × 28 indicator, a 60 dp bar, and a
 * height that responds to the text size instead of cutting it off.
 */
@Composable
fun ThykraNavigationBar(
    state: NavBarState,
    onSelect: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val labelStyle = navLabelStyle()

    // The bar is 60 dp at the default text size and grows only when the label genuinely
    // needs the room — measured from the style's own line height, so it tracks the
    // reader's font scale exactly rather than guessing at a multiplier.
    val labelHeight = with(density) { labelStyle.lineHeight.toDp() }
    val contentHeight = BarVerticalPadding * 2 + IndicatorHeight + IconLabelGap + labelHeight
    val barHeight = maxOf(MinBarHeight, contentHeight)

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = scheme.outlineVariant)
        Surface(color = scheme.surface, contentColor = scheme.onSurface, tonalElevation = 0.dp) {
            Box(
                // Applied *outside* the height so the inset is added below the bar rather
                // than eaten out of it. The other order leaves a gesture-nav device with
                // roughly 36 dp of usable bar and clips icon and label alike.
                //
                // navigationBars rather than safeDrawing: this also pads the side bar in
                // landscape, and the bar must never rise with the keyboard.
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = BarMaxWidth)
                        .fillMaxWidth()
                        .height(barHeight),
                ) {
                    RootTab.entries.forEach { tab ->
                        NavTab(
                            tab = tab,
                            selected = tab == state.selected,
                            state = state,
                            labelStyle = labelStyle,
                            onSelect = { onSelect(tab) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * `titleSmall` at the 12 sp the board calls for, with the same font-scale ceiling the
 * display slots use. Without the cap a 2x text setting turns "Activity" into an ellipsis
 * on a narrow phone; with it the label keeps growing to 1.5x and then holds, and the bar
 * has grown to match.
 */
@Composable
private fun navLabelStyle(): TextStyle {
    val fontScale = LocalDensity.current.fontScale
    val cap = (fontScale.coerceAtMost(DisplayFontScaleCap) / fontScale).coerceIn(0.01f, 1f)
    return MaterialTheme.typography.titleSmall.copy(
        fontSize = 12.sp * cap,
        lineHeight = 16.sp * cap,
    )
}

@Composable
private fun RowScope.NavTab(
    tab: RootTab,
    selected: Boolean,
    state: NavBarState,
    labelStyle: TextStyle,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    // Resolved out here: the semantics lambda is not a composable scope.
    val unseen = stringResource(Res.string.nav_activity_unseen)

    val indicator by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else Color.Transparent,
        animationSpec = thykraTween(),
        label = "navIndicator",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = thykraTween(),
        label = "navIcon",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onSelect,
            )
            .semantics {
                if (tab == RootTab.Activity && state.activityDot) {
                    stateDescription = unseen
                }
            }
            .padding(vertical = BarVerticalPadding, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = IndicatorWidth, height = IndicatorHeight)
                .background(indicator, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            TabIcon(tab = tab, selected = selected, state = state, tint = contentColor)
        }
        Spacer(Modifier.height(IconLabelGap))
        Text(
            text = stringResource(tab.label),
            style = labelStyle,
            // The selected label stays onSurface — only the glyph takes the accent, so a
            // colour-blind reader still has the indicator pill to go on.
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TabIcon(tab: RootTab, selected: Boolean, state: NavBarState, tint: Color) {
    val scheme = MaterialTheme.colorScheme

    Box {
        when (tab) {
            RootTab.Me -> {
                val avatar = state.meAvatar
                if (avatar != null) {
                    Box(
                        modifier = Modifier
                            .size(IconSize)
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
                        tint = tint,
                        modifier = Modifier.size(IconSize),
                    )
                }
            }

            else -> Icon(
                imageVector = tab.glyph(selected),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(IconSize),
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
