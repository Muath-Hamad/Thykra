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
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.common_back
import com.jameeli.thykra.resources.common_more_options
import com.jameeli.thykra.resources.common_photo_one
import com.jameeli.thykra.resources.common_photos_count
import com.jameeli.thykra.resources.common_remove
import com.jameeli.thykra.resources.common_video_one
import com.jameeli.thykra.resources.settings_and
import com.jameeli.thykra.resources.settings_block
import com.jameeli.thykra.resources.settings_block_confirm_body
import com.jameeli.thykra.resources.settings_block_confirm_title
import com.jameeli.thykra.resources.settings_delete_confirm_title
import com.jameeli.thykra.resources.settings_delete_gone
import com.jameeli.thykra.resources.settings_delete_nothing
import com.jameeli.thykra.resources.settings_delete_trip
import com.jameeli.thykra.resources.settings_delete_undone
import com.jameeli.thykra.resources.settings_expired
import com.jameeli.thykra.resources.settings_expires_days
import com.jameeli.thykra.resources.settings_expires_hours
import com.jameeli.thykra.resources.settings_expires_tomorrow
import com.jameeli.thykra.resources.settings_expiry_days
import com.jameeli.thykra.resources.settings_joined_many
import com.jameeli.thykra.resources.settings_joined_none
import com.jameeli.thykra.resources.settings_joined_one
import com.jameeli.thykra.resources.settings_keep_it
import com.jameeli.thykra.resources.settings_leave
import com.jameeli.thykra.resources.settings_leave_confirm_body
import com.jameeli.thykra.resources.settings_leave_confirm_title
import com.jameeli.thykra.resources.settings_leave_note
import com.jameeli.thykra.resources.settings_leave_trip
import com.jameeli.thykra.resources.settings_new_link
import com.jameeli.thykra.resources.settings_remove_confirm_body
import com.jameeli.thykra.resources.settings_remove_confirm_title
import com.jameeli.thykra.resources.settings_revoke
import com.jameeli.thykra.resources.settings_revoke_many
import com.jameeli.thykra.resources.settings_revoke_none
import com.jameeli.thykra.resources.settings_revoke_one
import com.jameeli.thykra.resources.settings_revoke_title
import com.jameeli.thykra.resources.settings_show_more
import com.jameeli.thykra.resources.settings_title
import com.jameeli.thykra.resources.settings_unblock
import com.jameeli.thykra.resources.trip_invite_friends
import com.jameeli.thykra.resources.trip_invite_supporting
import com.jameeli.thykra.resources.trip_link_owner_only
import com.jameeli.thykra.resources.trip_link_public
import com.jameeli.thykra.resources.trip_videos_count
import org.jetbrains.compose.resources.stringResource
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
            title = { Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineSmall) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(ThykraIcons.Back, contentDescription = stringResource(Res.string.common_back))
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
                            text = stringResource(Res.string.trip_link_public),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!isOwner) {
                            Text(
                                text = stringResource(Res.string.trip_link_owner_only),
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
                        label = stringResource(Res.string.settings_show_more, members.size - visibleMembers.size),
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
                            label = stringResource(Res.string.settings_new_link),
                            onClick = { viewModel.createInviteLink(albumId, expiryDays) },
                            variant = ThykraButtonVariant.Tonal,
                            icon = ThykraIcons.Link,
                        )
                        AssistChip(
                            label = stringResource(Res.string.settings_expiry_days, expiryDays),
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
                                label = stringResource(Res.string.settings_unblock),
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
                        label = stringResource(Res.string.trip_invite_friends),
                        onClick = {
                            viewModel.createInviteLink(albumId, 7) { link ->
                                shareText("https://thykra.com/invite/${link.token}")
                            }
                        },
                        icon = ThykraIcons.PersonAdd,
                        supporting = stringResource(Res.string.trip_invite_supporting),
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
                        label = stringResource(Res.string.settings_delete_trip),
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
                        label = stringResource(Res.string.settings_leave_trip),
                        onClick = { pendingConfirm = PendingConfirm.LeaveTrip },
                        variant = ThykraButtonVariant.Text,
                        destructive = true,
                    )
                    Text(
                        text = stringResource(Res.string.settings_leave_note),
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
                title = stringResource(Res.string.settings_delete_confirm_title, album?.title.orEmpty()),
                body = deleteConsequence(album?.mediaCount ?: 0, album?.videoCount ?: 0) +
                    stringResource(Res.string.settings_delete_undone),
                confirmLabel = stringResource(Res.string.settings_delete_trip),
                dismissLabel = stringResource(Res.string.settings_keep_it),
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
                title = stringResource(Res.string.settings_leave_confirm_title, album?.title.orEmpty()),
                body = stringResource(Res.string.settings_leave_confirm_body),
                confirmLabel = stringResource(Res.string.settings_leave),
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
                title = stringResource(
                    Res.string.settings_remove_confirm_title,
                    confirm.member.displayName.substringBefore(' '),
                ),
                body = stringResource(Res.string.settings_remove_confirm_body, confirm.member.displayName),
                confirmLabel = stringResource(Res.string.common_remove),
                onConfirm = {
                    dismiss()
                    viewModel.removeMember(albumId, confirm.member.userId, confirm.member.displayName)
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.BlockMember -> ConfirmDialog(
                title = stringResource(
                    Res.string.settings_block_confirm_title,
                    confirm.member.displayName.substringBefore(' '),
                ),
                body = stringResource(Res.string.settings_block_confirm_body, confirm.member.displayName),
                confirmLabel = stringResource(
                    Res.string.settings_block,
                    confirm.member.displayName.substringBefore(' '),
                ),
                onConfirm = {
                    dismiss()
                    viewModel.blockMember(albumId, confirm.member.userId, confirm.member.displayName)
                },
                onDismiss = dismiss,
            )

            is PendingConfirm.RevokeLink -> ConfirmDialog(
                title = stringResource(Res.string.settings_revoke_title),
                body = joinConsequence(confirm.invite.joinCount),
                confirmLabel = stringResource(Res.string.settings_revoke),
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
                Icon(ThykraIcons.More, contentDescription = stringResource(Res.string.common_more_options))
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
        ThykraButton(
            stringResource(Res.string.settings_revoke),
            onRevoke,
            variant = ThykraButtonVariant.Text,
            destructive = true,
        )
    }
}

@Composable
private fun joinLabel(joinCount: Int) = when (joinCount) {
    0 -> stringResource(Res.string.settings_joined_none)
    1 -> stringResource(Res.string.settings_joined_one)
    else -> stringResource(Res.string.settings_joined_many, joinCount)
}

@Composable
private fun expiryLabel(hoursLeft: Int) = when {
    hoursLeft <= 0 -> stringResource(Res.string.settings_expired)
    hoursLeft < 24 -> stringResource(Res.string.settings_expires_hours, hoursLeft)
    hoursLeft < 48 -> stringResource(Res.string.settings_expires_tomorrow)
    else -> stringResource(Res.string.settings_expires_days, hoursLeft / 24)
}

@Composable
private fun joinConsequence(joinCount: Int) = when (joinCount) {
    0 -> stringResource(Res.string.settings_revoke_none)
    1 -> stringResource(Res.string.settings_revoke_one)
    else -> stringResource(Res.string.settings_revoke_many, joinCount)
}

/**
 * "84 photos and 3 videos will be gone for everyone."
 *
 * The conjunction is a key rather than a literal " and ": joining two phrases is not the
 * same operation in every language, and singular is a separate string rather than a
 * count, because "1 photos" is wrong in English and the plural rules differ in Arabic.
 */
@Composable
private fun deleteConsequence(photos: Int, videos: Int): String {
    val parts = mutableListOf<String>()
    if (photos > 0) {
        parts += if (photos == 1) {
            stringResource(Res.string.common_photo_one)
        } else {
            stringResource(Res.string.common_photos_count, photos)
        }
    }
    if (videos > 0) {
        parts += if (videos == 1) {
            stringResource(Res.string.common_video_one)
        } else {
            stringResource(Res.string.trip_videos_count, videos)
        }
    }
    if (parts.isEmpty()) return stringResource(Res.string.settings_delete_nothing)
    val subject = if (parts.size == 2) {
        stringResource(Res.string.settings_and, parts[0], parts[1])
    } else {
        parts[0]
    }
    return stringResource(Res.string.settings_delete_gone, subject)
}
