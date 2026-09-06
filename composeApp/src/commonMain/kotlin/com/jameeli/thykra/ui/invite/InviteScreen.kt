package com.jameeli.thykra.ui.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.model.InvitePreviewDto
import com.jameeli.thykra.model.InviteStatus
import com.jameeli.thykra.ui.kit.AvatarSize
import com.jameeli.thykra.ui.kit.AvatarStack
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.Stamp
import com.jameeli.thykra.ui.kit.StampTone
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonSpec
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.kit.skeleton
import com.jameeli.thykra.ui.share.shareText
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykra

/**
 * Design part 3 §05. The growth loop.
 *
 * One job: make the trip look like somewhere you were, and put Join under the thumb. It
 * is reachable signed-out, because asking someone to sign in before they can see what
 * they were invited to loses them.
 */
@Composable
fun InviteScreen(
    token: String,
    viewModel: InviteViewModel,
    signedIn: Boolean,
    onJoined: (String) -> Unit,
    onSignIn: () -> Unit,
    onClose: () -> Unit,
    onOpenTrip: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val haptic = rememberHaptics()

    LaunchedEffect(token) { viewModel.load(token) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (val current = state) {
            is InviteUiState.Loading -> InviteSkeleton()

            is InviteUiState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    headline = clayPhrase("Couldn't check ", "this link."),
                    body = if (current.offline) {
                        "You're offline. Try again when you're back."
                    } else {
                        current.message
                    },
                    glyph = if (current.offline) EmptyGlyph.Offline else EmptyGlyph.Plate,
                    primary = ThykraButtonSpec(
                        label = "Try again",
                        onClick = { viewModel.load(token) },
                        variant = ThykraButtonVariant.Outlined,
                        icon = ThykraIcons.Retry,
                    ),
                )
            }

            is InviteUiState.Joining -> InvitePreviewBody(
                preview = null,
                signedIn = signedIn,
                joining = true,
                onJoin = {},
                onSignIn = onSignIn,
                onClose = onClose,
                onOpenTrip = onOpenTrip,
                onMessageInviter = {},
            )

            is InviteUiState.Joined -> JoinedCelebration(
                albumTitle = current.album.title,
                mediaCount = current.album.mediaCount,
                onAddPhotos = { onJoined(current.album.id) },
                onJustLook = { onJoined(current.album.id) },
            )

            is InviteUiState.Ready -> InvitePreviewBody(
                preview = current.preview,
                signedIn = signedIn,
                joining = false,
                onJoin = {
                    haptic(HapticKind.Confirm)
                    viewModel.join(token) { }
                },
                onSignIn = onSignIn,
                onClose = onClose,
                onOpenTrip = onOpenTrip,
                onMessageInviter = {
                    val name = current.preview.invitedBy?.displayName ?: "them"
                    shareText("Could you send me a fresh Thykra link? Thanks, $name.")
                },
            )
        }

        CloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun InvitePreviewBody(
    preview: InvitePreviewDto?,
    signedIn: Boolean,
    joining: Boolean,
    onJoin: () -> Unit,
    onSignIn: () -> Unit,
    onClose: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onMessageInviter: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val status = preview?.status ?: InviteStatus.VALID
    val album = preview?.album

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 300 dp of cover — 35% of an 852 dp window — with ink behind the status bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            val cover = preview?.previewMedia?.firstOrNull()?.thumbnailUrl ?: album?.coverUrl
            if (cover.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceVariant, PlateShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ThykraIcons.Trips,
                        contentDescription = null,
                        tint = extended.textMeta,
                        modifier = Modifier.size(40.dp),
                    )
                }
            } else {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.42f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // The Stamp overlaps the plate by 22 dp.
            preview?.invitedBy?.let { inviter ->
                Box(modifier = Modifier.offset(y = (-22).dp)) {
                    Stamp(
                        eyebrow = if (status == InviteStatus.EXPIRED) "Link" else "Invited by",
                        name = if (status == InviteStatus.EXPIRED) "Expired" else inviter.displayName,
                        tone = if (status == InviteStatus.EXPIRED) StampTone.Warning else StampTone.Clay,
                    )
                }
            }

            when (status) {
                InviteStatus.VALID -> ValidBody(preview, album?.title)
                InviteStatus.ALREADY_MEMBER -> DeadEndBody(
                    headline = clayPhrase("You're ", "already in."),
                    body = "This trip is already yours to open.",
                )
                InviteStatus.EXPIRED -> DeadEndBody(
                    headline = clayPhrase("Link ", "expired."),
                    body = "This link has run out. Ask ${preview?.invitedBy?.displayName ?: "whoever sent it"} " +
                        "for a fresh one — the trip is still there.",
                )
                InviteStatus.REVOKED, InviteStatus.BLOCKED -> DeadEndBody(
                    // Byte-identical for a blocked person and for an unknown token.
                    headline = clayPhrase("This link ", "isn't available."),
                    body = "Ask whoever sent it for a new one.",
                )
            }

            if (status == InviteStatus.VALID || status == InviteStatus.ALREADY_MEMBER) {
                Spacer(Modifier.height(16.dp))
                PeopleRow(preview)
            }
        }

        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(32.dp))

        InviteActions(
            status = status,
            signedIn = signedIn,
            joining = joining,
            inviterName = preview?.invitedBy?.displayName,
            expiresInDays = null,
            onJoin = onJoin,
            onSignIn = onSignIn,
            onClose = onClose,
            onOpenTrip = { album?.id?.let(onOpenTrip) },
            onMessageInviter = onMessageInviter,
        )
    }
}

@Composable
private fun ValidBody(preview: InvitePreviewDto?, title: String?) {
    Text(
        text = clayPhrase("", title.orEmpty()),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        // Fixed copy: the preview payload carries no description, and inventing one from
        // the trip's own would leak more than the invite should.
        text = "Everyone's photos in one place. Join and yours land in the right day " +
            "on their own.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.thykra.textMeta,
    )
}

@Composable
private fun DeadEndBody(
    headline: androidx.compose.ui.text.AnnotatedString,
    body: String,
) {
    Text(
        text = headline,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.thykra.textMeta,
    )
}

@Composable
private fun PeopleRow(preview: InvitePreviewDto?) {
    val album = preview?.album ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarStack(
            users = album.previewMembers.map { it.toAvatarUser() },
            max = 3,
            size = AvatarSize.Sm,
            ringColor = MaterialTheme.colorScheme.surface,
            totalCount = album.memberCount,
        )
        Text(
            text = buildString {
                append(if (album.memberCount == 1) "1 person" else "${album.memberCount} people")
                val photos = preview.mediaCount ?: album.mediaCount
                if (photos > 0) append(" · $photos photos")
                if (album.videoCount > 0) append(" · ${album.videoCount} videos")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.thykra.textMeta,
        )
    }
}

/** Bottom-anchored, 32 dp above the nav inset — the only thing in the thumb's reach. */
@Composable
private fun InviteActions(
    status: InviteStatus,
    signedIn: Boolean,
    joining: Boolean,
    inviterName: String?,
    expiresInDays: Int?,
    onJoin: () -> Unit,
    onSignIn: () -> Unit,
    onClose: () -> Unit,
    onOpenTrip: () -> Unit,
    onMessageInviter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (status) {
            InviteStatus.VALID -> {
                ThykraButton(
                    label = if (signedIn) "Join this trip" else "Sign in to join",
                    onClick = if (signedIn) onJoin else onSignIn,
                    variant = ThykraButtonVariant.People,
                    loading = joining,
                    modifier = Modifier.fillMaxWidth(),
                )
                ThykraButton("Not now", onClose, variant = ThykraButtonVariant.Text)
                Text(
                    text = if (signedIn) {
                        expiresInDays?.let { "Link open for $it more days" }.orEmpty()
                    } else {
                        "Google or Apple · you'll land right back here"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.thykra.textMeta,
                    textAlign = TextAlign.Center,
                )
            }

            InviteStatus.ALREADY_MEMBER -> ThykraButton(
                label = "Open the trip",
                onClick = onOpenTrip,
                modifier = Modifier.fillMaxWidth(),
            )

            InviteStatus.EXPIRED -> {
                ThykraButton(
                    label = "Message ${inviterName?.substringBefore(' ') ?: "them"}",
                    onClick = onMessageInviter,
                    variant = ThykraButtonVariant.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
                ThykraButton("Not now", onClose, variant = ThykraButtonVariant.Text)
            }

            // Revoked and blocked offer nothing but a way out — there is nothing to do here.
            InviteStatus.REVOKED, InviteStatus.BLOCKED -> ThykraButton(
                label = "Go to my trips",
                onClick = onClose,
                variant = ThykraButtonVariant.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The second of the app's two celebrations. */
@Composable
private fun JoinedCelebration(
    albumTitle: String,
    mediaCount: Int,
    onAddPhotos: () -> Unit,
    onJustLook: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = clayPhrase("You're ", "in."),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (mediaCount > 0) {
                "$albumTitle has $mediaCount photos waiting. " +
                    "Yours from the trip will sort themselves by day."
            } else {
                "$albumTitle is yours now. Add the first photos and it starts telling " +
                    "itself by day."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.thykra.textMeta,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ThykraButton(
            label = "Add my photos",
            onClick = onAddPhotos,
            icon = ThykraIcons.Plus,
            modifier = Modifier.fillMaxWidth(),
        )
        ThykraButton("Just look for now", onJustLook, variant = ThykraButtonVariant.Text)
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val extended = MaterialTheme.thykra
    Box(
        modifier = modifier
            .safeDrawingPadding()
            .padding(8.dp)
            .size(40.dp)
            .background(
                extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                MaterialTheme.shapes.small,
            )
            .clickable(role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ThykraIcons.Close,
            contentDescription = "Close",
            tint = extended.onScrim,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InviteSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .skeleton(),
        )
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(36.dp)
                    .skeleton(MaterialTheme.shapes.extraSmall),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .skeleton(MaterialTheme.shapes.extraSmall),
            )
        }
    }
}
