package com.jameeli.thykra.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.InviteLinkDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.ui.kit.AssistChip
import com.jameeli.thykra.ui.kit.Avatar
import com.jameeli.thykra.ui.kit.AvatarSize
import com.jameeli.thykra.ui.kit.BadgeTone
import com.jameeli.thykra.ui.kit.ConfirmDialog
import com.jameeli.thykra.ui.kit.OfflineBanner
import com.jameeli.thykra.ui.kit.SheetAction
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.ThykraBadge
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.ToastTone
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.share.shareText
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.nowMillis

/**
 * Design part 3 §09.
 *
 * Two screens, one set of rows. Delete and Leave sit at the very end, after a divider and
 * 24 dp of nothing — deliberately outside the thumb arc, because they are the two things
 * you do once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSettingsScreen(
    albumId: String,
    viewModel: TripSettingsViewModel,
    onBack: () -> Unit,
    onLeftOrDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val album by viewModel.album.collectAsState()
    val members by viewModel.members.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val invites by viewModel.invites.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val message by viewModel.message.collectAsState()
    val chrome = LocalThykraChrome.current

    var showAllMembers by remember { mutableStateOf(false) }
    var memberSheet by remember { mutableStateOf<AlbumMemberDto?>(null) }
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }
    var editOpen by remember { mutableStateOf(false) }
    var expiryDays by remember { mutableStateOf(7) }

    LaunchedEffect(albumId) { viewModel.load(albumId) }

    LaunchedEffect(message) {
        message?.let {
            chrome.toast.show(it, if (it.startsWith("Couldn't")) ToastTone.Error else ToastTone.Neutral)
            viewModel.consumeMessage()
        }
    }

    val isOwner = viewModel.isOwner
    val visibleMembers = if (showAllMembers) members else members.take(4)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(ThykraIcons.Back, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        OfflineBanner(visible = !connected)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            item("trip") {
                SectionHeading("Trip")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album?.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        album?.description?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.thykra.textMeta,
                            )
                        }
                    }
                    if (isOwner) {
                        ThykraButton("Edit", { editOpen = true }, variant = ThykraButtonVariant.Text)
                    }
                }
            }

            item("visibility") {
                SectionDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (album?.visibility == AlbumVisibility.LINK_SHARED) {
                            ThykraIcons.Globe
                        } else {
                            ThykraIcons.Lock
                        },
                        contentDescription = null,
                        tint = MaterialTheme.thykra.textMeta,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
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
                                color = MaterialTheme.thykra.textMeta,
                            )
                        }
                    }
                    if (isOwner) {
                        Switch(
                            checked = album?.visibility == AlbumVisibility.LINK_SHARED,
                            onCheckedChange = { on ->
                                viewModel.setVisibility(
                                    albumId,
                                    if (on) AlbumVisibility.LINK_SHARED else AlbumVisibility.PRIVATE,
                                )
                            },
                        )
                    }
                }
            }

            item("people") {
                SectionDivider()
                SectionHeading("People · ${members.size}")
            }

            items(visibleMembers, key = { it.userId }) { member ->
                MemberRow(
                    member = member,
                    isSelf = member.userId == currentUserId,
                    canManage = isOwner && member.userId != currentUserId,
                    onMore = { memberSheet = member },
                )
            }

            if (members.size > visibleMembers.size) {
                item("showMore") {
                    ThykraButton(
                        label = "Show ${members.size - visibleMembers.size} more",
                        onClick = { showAllMembers = true },
                        variant = ThykraButtonVariant.Text,
                    )
                }
            }

            if (isOwner) {
                item("invites") {
                    SectionDivider()
                    SectionHeading("Invite links · ${invites.size} active")
                }

                items(invites, key = { it.token }) { invite ->
                    InviteRow(
                        invite = invite,
                        onShare = { shareText("https://thykra.com/invite/${invite.token}") },
                        onRevoke = {
                            pendingConfirm = PendingConfirm.RevokeLink(invite)
                        },
                    )
                }

                item("newLink") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThykraButton(
                            label = "New link",
                            onClick = { viewModel.createInviteLink(albumId, expiryDays) },
                            variant = ThykraButtonVariant.Tonal,
                            icon = ThykraIcons.Link,
                        )
                        AssistChip(
                            label = "$expiryDays days",
                            onClick = {
                                expiryDays = when (expiryDays) {
                                    1 -> 7
                                    7 -> 30
                                    else -> 1
                                }
                            },
                        )
                    }
                }

                if (blocked.isNotEmpty()) {
                    item("blocked") {
                        SectionDivider()
                        SectionHeading("Blocked · ${blocked.size}")
                    }
                    items(blocked, key = { it.userId }) { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = person.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            ThykraButton(
                                label = "Unblock",
                                onClick = {
                                    viewModel.unblockMember(albumId, person.userId, person.displayName)
                                },
                                variant = ThykraButtonVariant.Text,
                            )
                        }
                    }
                }
            } else {
                item("yourLink") {
                    SectionDivider()
                    SectionHeading("Your link")
                    SheetAction(
                        label = "Invite friends",
                        onClick = {
                            viewModel.createInviteLink(albumId, 7) { link ->
                                shareText("https://thykra.com/invite/${link.token}")
                            }
                        },
                        icon = ThykraIcons.PersonAdd,
                        supporting = "Creates a 7-day link you can share",
                    )
                }
            }

            // Outside the thumb arc, on purpose.
            item("danger") {
                Spacer(Modifier.height(24.dp))
                SectionDivider()
                Spacer(Modifier.height(24.dp))
                if (isOwner) {
                    ThykraButton(
                        label = "Delete trip",
                        onClick = { pendingConfirm = PendingConfirm.DeleteTrip },
                        variant = ThykraButtonVariant.Text,
                        destructive = true,
                    )
                    Text(
                        text = deleteConsequence(album?.mediaCount ?: 0, album?.videoCount ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.thykra.textMeta,
                    )
                } else if (viewModel.role != null) {
                    // Owners see no Leave: the row does not exist rather than being disabled.
                    ThykraButton(
                        label = "Leave trip",
                        onClick = { pendingConfirm = PendingConfirm.LeaveTrip },
                        variant = ThykraButtonVariant.Text,
                        destructive = true,
                    )
                    Text(
                        text = "Your photos stay in the trip.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.thykra.textMeta,
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }

    memberSheet?.let { member ->
        ThykraSheet(
            onDismiss = { memberSheet = null },
            header = {
                MemberRow(member = member, isSelf = false, canManage = false, onMore = {})
            },
        ) {
            SheetAction("View their photos", { memberSheet = null }, icon = ThykraIcons.Grid)
            if (viewModel.isOwner) {
                SheetDivider()
                SheetAction("Remove from trip", {
                    memberSheet = null
                    pendingConfirm = PendingConfirm.RemoveMember(member)
                }, destructive = true)
                SheetDivider()
                SheetAction("Block", {
                    memberSheet = null
                    pendingConfirm = PendingConfirm.BlockMember(member)
                }, destructive = true)
            }
        }
    }

    pendingConfirm?.let { confirm ->
        val dismiss = { pendingConfirm = null }
        when (confirm) {
            is PendingConfirm.DeleteTrip -> ConfirmDialog(
                title = "Delete ${album?.title.orEmpty()}?",
                body = deleteConsequence(album?.mediaCount ?: 0, album?.videoCount ?: 0) +
                    " This can't be undone.",
                confirmLabel = "Delete trip",
                dismissLabel = "Keep it",
                onConfirm = {
                    dismiss()
                    viewModel.deleteTrip(albumId) {
                        chrome.toast.show("${album?.title.orEmpty()} deleted")
                        onLeftOrDeleted()
                    }
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.LeaveTrip -> ConfirmDialog(
                title = "Leave ${album?.title.orEmpty()}?",
                body = "Your photos stay in the trip. You'll need a new link to come back.",
                confirmLabel = "Leave",
                onConfirm = {
                    dismiss()
                    viewModel.leaveTrip(albumId) {
                        chrome.toast.show("You left ${album?.title.orEmpty()}")
                        onLeftOrDeleted()
                    }
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.RemoveMember -> ConfirmDialog(
                title = "Remove ${confirm.member.displayName.substringBefore(' ')}?",
                body = "${confirm.member.displayName} leaves the trip. Their photos stay.",
                confirmLabel = "Remove",
                onConfirm = {
                    dismiss()
                    viewModel.removeMember(albumId, confirm.member.userId, confirm.member.displayName)
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.BlockMember -> ConfirmDialog(
                title = "Block ${confirm.member.displayName.substringBefore(' ')}?",
                body = "${confirm.member.displayName} leaves the trip and can't rejoin with " +
                    "any link. Their photos stay.",
                confirmLabel = "Block ${confirm.member.displayName.substringBefore(' ')}",
                onConfirm = {
                    dismiss()
                    viewModel.blockMember(albumId, confirm.member.userId, confirm.member.displayName)
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.RevokeLink -> ConfirmDialog(
                title = "Revoke this link?",
                body = joinConsequence(confirm.invite.joinCount),
                confirmLabel = "Revoke",
                onConfirm = {
                    dismiss()
                    viewModel.revokeInviteLink(albumId, confirm.invite.token)
                },
                onDismiss = dismiss,
            )
        }
    }

    if (editOpen) {
        EditTripSheet(
            initialTitle = album?.title.orEmpty(),
            initialDescription = album?.description.orEmpty(),
            onDismiss = { editOpen = false },
            onSave = { title, description ->
                editOpen = false
                viewModel.updateTrip(albumId, title, description)
            },
        )
    }
}

private sealed interface PendingConfirm {
    data object DeleteTrip : PendingConfirm
    data object LeaveTrip : PendingConfirm
    data class RemoveMember(val member: AlbumMemberDto) : PendingConfirm
    data class BlockMember(val member: AlbumMemberDto) : PendingConfirm
    data class RevokeLink(val invite: InviteLinkDto) : PendingConfirm
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun MemberRow(
    member: AlbumMemberDto,
    isSelf: Boolean,
    canManage: Boolean,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            user = member.toAvatarUser(),
            size = AvatarSize.Md,
            ownerBadge = member.role == MemberRole.OWNER,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSelf) "${member.displayName} · you" else member.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.role.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.thykra.textMeta,
            )
        }
        ThykraBadge(
            label = member.role.name.lowercase().replaceFirstChar { it.uppercase() },
            tone = if (member.role == MemberRole.VIEWER) BadgeTone.Muted else BadgeTone.People,
        )
        if (canManage) {
            IconButton(onClick = onMore) {
                Icon(ThykraIcons.More, contentDescription = "More options")
            }
        }
    }
}

@Composable
private fun InviteRow(
    invite: InviteLinkDto,
    onShare: () -> Unit,
    onRevoke: () -> Unit,
) {
    val nowMs = remember { nowMillis() }
    val hoursLeft = ((invite.expiresAt.toEpochMilliseconds() - nowMs) / 3_600_000).toInt()
    val expiringSoon = hoursLeft in 0..48

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable(role = Role.Button, onClick = onShare),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = ThykraIcons.Link,
            contentDescription = null,
            tint = MaterialTheme.thykra.textMeta,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = joinLabel(invite.joinCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = expiryLabel(hoursLeft),
                style = MaterialTheme.typography.bodySmall,
                color = if (expiringSoon) MaterialTheme.thykra.warning else MaterialTheme.thykra.textMeta,
            )
        }
        ThykraButton("Revoke", onRevoke, variant = ThykraButtonVariant.Text, destructive = true)
    }
}

private fun joinLabel(joinCount: Int) = when (joinCount) {
    0 -> "No one has joined yet"
    1 -> "1 person joined"
    else -> "$joinCount joined"
}

private fun expiryLabel(hoursLeft: Int) = when {
    hoursLeft <= 0 -> "Expired"
    hoursLeft < 24 -> "Expires in $hoursLeft hours"
    hoursLeft < 48 -> "Expires tomorrow"
    else -> "Expires in ${hoursLeft / 24} days"
}

private fun joinConsequence(joinCount: Int) = when (joinCount) {
    0 -> "No one has joined through it. Nothing else changes."
    1 -> "1 person already joined through it; they stay."
    else -> "$joinCount people already joined through it; they stay."
}

private fun deleteConsequence(photos: Int, videos: Int): String {
    val parts = mutableListOf<String>()
    if (photos > 0) parts += "$photos ${if (photos == 1) "photo" else "photos"}"
    if (videos > 0) parts += "$videos ${if (videos == 1) "video" else "videos"}"
    if (parts.isEmpty()) return "This trip has nothing in it yet."
    return "${parts.joinToString(" and ")} will be gone for everyone."
}
