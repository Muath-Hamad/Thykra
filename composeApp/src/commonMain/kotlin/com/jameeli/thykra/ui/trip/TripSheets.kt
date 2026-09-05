package com.jameeli.thykra.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.ui.kit.SheetAction
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonSize
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.share.copyToClipboard
import com.jameeli.thykra.ui.share.shareText
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/** The ⋯ sheet from part 3 §06. Four ways out of the trip, and nothing destructive. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMoreSheet(
    onDismiss: () -> Unit,
    onActivity: () -> Unit,
    onRecaps: () -> Unit,
    onSelectPhotos: () -> Unit,
    onSettings: () -> Unit,
) {
    ThykraSheet(onDismiss = onDismiss) {
        SheetAction("Trip activity", onActivity, icon = ThykraIcons.Activity)
        SheetDivider()
        SheetAction("Recaps", onRecaps, icon = ThykraIcons.Recaps)
        SheetDivider()
        SheetAction("Select photos", onSelectPhotos, icon = ThykraIcons.Grid)
        SheetDivider()
        SheetAction("Settings", onSettings, icon = ThykraIcons.Settings)
    }
}

/**
 * The share sheet from part 2 §4.4.
 *
 * The visibility switch is the owner's only; a member sees a line instead, because a
 * control that does nothing is worse than a sentence that explains why.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTripSheet(
    album: AlbumDto,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onEnsureLink: ((String) -> Unit) -> Unit,
    onSetVisibility: (AlbumVisibility) -> Unit,
    onCopied: () -> Unit,
) {
    val extended = MaterialTheme.thykra
    val linkShared = album.visibility == AlbumVisibility.LINK_SHARED

    ThykraSheet(onDismiss = onDismiss, title = "Share ${album.title}") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anyone with the link can view",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!isOwner) {
                    Text(
                        text = "Only the owner can change this",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMeta,
                    )
                }
            }
            if (isOwner) {
                Switch(
                    checked = linkShared,
                    onCheckedChange = { on ->
                        onSetVisibility(
                            if (on) AlbumVisibility.LINK_SHARED else AlbumVisibility.PRIVATE,
                        )
                    },
                )
            }
        }

        SheetDivider()

        SheetAction("Share link", {
            onEnsureLink { url -> shareText(url) }
        }, icon = ThykraIcons.Share)

        SheetDivider()

        SheetAction("Copy link", {
            onEnsureLink { url ->
                copyToClipboard(url)
                onCopied()
            }
        }, icon = ThykraIcons.Copy)

        SheetDivider()

        SheetAction(
            label = "Invite friends",
            onClick = { onEnsureLink { url -> shareText(url) } },
            icon = ThykraIcons.PersonAdd,
            supporting = "Creates a 7-day link you can share",
        )
    }
}

/**
 * Selection mode's bottom bar. It replaces the action bar rather than sitting beside it,
 * so the thumb reaches the same place for a different job.
 *
 * Remove is here only when it is allowed: your own photographs, or anything if you own
 * the trip.
 */
@Composable
fun SelectionActionBar(
    count: Int,
    canRemove: Boolean,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .height(60.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThykraButton(
                label = if (count == 1) "Share 1" else "Share $count",
                onClick = onShare,
                icon = ThykraIcons.Share,
                size = ThykraButtonSize.Compact,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                ThykraButton(
                    label = "Remove",
                    onClick = onRemove,
                    variant = ThykraButtonVariant.Text,
                    destructive = true,
                    size = ThykraButtonSize.Compact,
                )
            }
        }
    }
}
