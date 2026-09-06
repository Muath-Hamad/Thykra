package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.common_more_options
import com.jameeli.thykra.resources.common_share
import com.jameeli.thykra.resources.trip_add_photos
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.ui.theme.LocalCompactWidth
import com.jameeli.thykra.ui.theme.ThykraIcons

/**
 * Part 2 §4.11 and part 3 §06.
 *
 * Inside a trip the nav bar gives its 60 dp to this, which is how the phone gets a
 * permanent, thumb-reachable Add photos without a FAB floating over photographs.
 *
 * A VIEWER sees Share full-width and no Add: the bar shows what you can do, rather than
 * showing what you cannot and greying it out.
 */
private val ActionHeight = 44.dp

/** 44 dp of button in a 60 dp bar, per the board — with room to grow for large text. */
private val BarHeight = 60.dp

@Composable
fun TripActionBar(
    role: MemberRole,
    onAddPhotos: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Below 380 dp Share drops its label and becomes an icon button; Add photos keeps
    // its label, because it is the primary.
    val compact = LocalCompactWidth.current
    val canAdd = role != MemberRole.VIEWER

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                // The inset goes outside the height so it is added below the bar, not
                // taken out of it. The other order leaves a gesture-nav device around
                // 36 dp of bar to seat 44 dp buttons in, and they clip.
                .navigationBarsPadding()
                .height(BarHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canAdd) {
                ThykraButton(
                    label = stringResource(Res.string.trip_add_photos),
                    onClick = onAddPhotos,
                    variant = ThykraButtonVariant.Filled,
                    icon = ThykraIcons.Plus,
                    modifier = Modifier
                        .weight(1f)
                        .height(ActionHeight),
                )
                if (compact) {
                    ActionIconButton(
                        icon = ThykraIcons.Share,
                        contentDescription = stringResource(Res.string.common_share),
                        onClick = onShare,
                        container = scheme.primaryContainer,
                        content = scheme.onPrimaryContainer,
                    )
                } else {
                    ThykraButton(
                        label = stringResource(Res.string.common_share),
                        onClick = onShare,
                        variant = ThykraButtonVariant.Tonal,
                        icon = ThykraIcons.Share,
                        modifier = Modifier.height(ActionHeight),
                    )
                }
            } else {
                ThykraButton(
                    label = stringResource(Res.string.common_share),
                    onClick = onShare,
                    variant = ThykraButtonVariant.Tonal,
                    icon = ThykraIcons.Share,
                    modifier = Modifier
                        .weight(1f)
                        .height(ActionHeight),
                )
            }

            ActionIconButton(
                icon = ThykraIcons.More,
                contentDescription = stringResource(Res.string.common_more_options),
                onClick = onMore,
                container = scheme.surface,
                content = scheme.onSurface,
                outlined = true,
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    outlined: Boolean = false,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(container, shape)
            .then(
                if (outlined) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
    }
}
