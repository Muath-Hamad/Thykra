package com.jameeli.thykra.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.permissions.AlbumPermissions
import com.jameeli.thykra.ui.media.rememberMediaPickerLauncher
import com.jameeli.thykra.ui.share.shareText
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.ThykraIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreenContent(
    albumId: String,
    viewModel: AlbumDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToViewer: (mediaId: String) -> Unit
) {
    val album by viewModel.album.collectAsState()
    val members by viewModel.members.collectAsState()
    val blockedMembers by viewModel.blockedMembers.collectAsState()
    val inviteLink by viewModel.inviteLink.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val media by viewModel.media.collectAsState()
    val uploads by viewModel.uploads.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val isOwner = remember(album, currentUserId) {
        val ownerId = album?.ownerId
        ownerId != null && currentUserId != null && ownerId == currentUserId
    }
    val ownerRole = if (isOwner) MemberRole.OWNER else null

    // Pending owner action against a single member (Remove or Block) — gated
    // through a confirmation dialog so an accidental tap doesn't kick someone.
    var pendingMemberAction by remember { mutableStateOf<PendingMemberAction?>(null) }

    val albumUploads = remember(uploads, albumId) {
        uploads.filter { it.albumId == albumId && it.status != UploadStatus.DONE }
    }

    var prevDoneCount by remember { mutableIntStateOf(0) }
    val doneCount = remember(uploads, albumId) {
        uploads.count { it.albumId == albumId && it.status == UploadStatus.DONE }
    }
    LaunchedEffect(doneCount) {
        if (doneCount > prevDoneCount) viewModel.refreshMedia(albumId)
        prevDoneCount = doneCount
    }

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    val pickerLauncher = rememberMediaPickerLauncher { files ->
        viewModel.uploadFiles(albumId, files)
    }

    Scaffold(
        containerColor = ThykraColors.WarmWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        album?.title ?: "Album",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = ThykraIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = ThykraColors.DeepNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThykraColors.WarmWhite,
                    titleContentColor = ThykraColors.DeepNavy
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pickerLauncher() },
                containerColor = ThykraColors.SkyBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = ThykraIcons.Add,
                    contentDescription = "Add Photos"
                )
            }
        }
    ) { padding ->
        if (isLoading && album == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ThykraColors.SkyBlue)
            }
        } else if (album != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                // Description
                item {
                    if (!album!!.description.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = album!!.description!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThykraColors.MutedSlate
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Media section header
                item {
                    Text(
                        text = "Photos & Videos (${media.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThykraColors.DeepNavy
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Media grid: rows of 3
                val rows = media.chunked(3)
                if (rows.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No photos yet. Tap + to add.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThykraColors.MutedSlate
                            )
                        }
                    }
                } else {
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ThykraColors.Sandy)
                                        .clickable { onNavigateToViewer(item.id) }
                                ) {
                                    val thumbUrl = (item.thumbnailUrl ?: item.url)
                                        .replace("http://localhost:8081", API_BASE_URL)
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = item.filename,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (item.type == MediaType.VIDEO) {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ThykraIcons.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining slots in the last row
                            repeat(3 - rowItems.size) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }

                // Upload progress
                if (albumUploads.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Uploading",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThykraColors.DeepNavy
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(albumUploads) { upload ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (upload.status == UploadStatus.FAILED) {
                                Icon(
                                    imageVector = ThykraIcons.Close,
                                    contentDescription = "Failed",
                                    modifier = Modifier.size(16.dp),
                                    tint = ThykraColors.SoftRed
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ThykraColors.SkyBlue
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = upload.filename,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThykraColors.DeepNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (upload.status) {
                                        UploadStatus.QUEUED -> "Queued"
                                        UploadStatus.UPLOADING -> "Uploading (attempt ${upload.attempt}/3)"
                                        UploadStatus.CONFIRMING -> "Processing"
                                        UploadStatus.DONE -> "Done"
                                        UploadStatus.FAILED -> upload.error ?: "Failed"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (upload.status == UploadStatus.FAILED)
                                        ThykraColors.SoftRed
                                    else
                                        ThykraColors.MutedSlate
                                )
                            }
                        }
                    }
                }

                // Members
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Members (${members.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThykraColors.DeepNavy
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Person avatar circle
                            Surface(
                                shape = CircleShape,
                                color = ThykraColors.SkyBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = ThykraIcons.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = ThykraColors.SkyBlue
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ThykraColors.DeepNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            // Role badge
                            val memberIsOwner = member.role == MemberRole.OWNER
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (memberIsOwner) ThykraColors.SkyBlue.copy(alpha = 0.15f)
                                else Color.Transparent,
                                contentColor = if (memberIsOwner) ThykraColors.SkyBlue
                                else ThykraColors.MutedSlate
                            ) {
                                Text(
                                    text = member.role.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            // Owner-only actions: Remove + Block. Owner cannot
                            // act on themselves and can never block another OWNER
                            // (server enforces this too).
                            if (
                                AlbumPermissions.canManageMembers(ownerRole) &&
                                !memberIsOwner &&
                                member.userId != currentUserId
                            ) {
                                IconButton(
                                    onClick = {
                                        pendingMemberAction = PendingMemberAction(
                                            userId = member.userId,
                                            displayName = member.displayName,
                                            kind = MemberActionKind.REMOVE
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = ThykraIcons.Close,
                                        contentDescription = "Remove ${member.displayName}",
                                        tint = ThykraColors.MutedSlate,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        pendingMemberAction = PendingMemberAction(
                                            userId = member.userId,
                                            displayName = member.displayName,
                                            kind = MemberActionKind.BLOCK
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = ThykraIcons.Block,
                                        contentDescription = "Block ${member.displayName}",
                                        tint = ThykraColors.SoftRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Blocked members (owner only). Hidden when nothing is blocked
                // so the section doesn't add visual weight to typical albums.
                if (
                    AlbumPermissions.canBlockMembers(ownerRole) &&
                    blockedMembers.isNotEmpty()
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Blocked (${blockedMembers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThykraColors.DeepNavy
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(blockedMembers) { blocked ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ThykraColors.SoftRed.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ThykraIcons.Block,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = ThykraColors.SoftRed
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = blocked.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ThykraColors.DeepNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.unblockMember(albumId, blocked.userId)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = ThykraColors.SkyBlue
                                    )
                                ) {
                                    Text(
                                        text = "Unblock",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Privacy / visibility (owner only)
                if (AlbumPermissions.canChangeVisibility(ownerRole)) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThykraColors.DeepNavy
                        )
                        Spacer(Modifier.height(8.dp))
                        VisibilitySection(
                            currentVisibility = album!!.visibility,
                            onChangeVisibility = { newVisibility ->
                                viewModel.setVisibility(albumId, newVisibility)
                            }
                        )
                    }
                }

                // Invite link with configurable expiry
                if (AlbumPermissions.canCreateInviteLink(ownerRole)) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Invite link",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThykraColors.DeepNavy
                        )
                        Spacer(Modifier.height(8.dp))
                        InviteLinkSection(
                            inviteLink = inviteLink,
                            onGenerate = { days ->
                                viewModel.createInviteLink(albumId, days)
                            }
                        )
                    }
                }

                // Share to external (link-shared albums only)
                item {
                    Spacer(Modifier.height(16.dp))
                    val shareEnabled = album!!.visibility == AlbumVisibility.LINK_SHARED
                    OutlinedButton(
                        onClick = {
                            shareText(publicAlbumUrl(albumId))
                        },
                        enabled = shareEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ThykraColors.SkyBlue
                        )
                    ) {
                        Icon(
                            imageVector = ThykraIcons.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Share album link")
                    }
                    if (!shareEnabled) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Make the album link-shared to share it externally.",
                            style = MaterialTheme.typography.labelMedium,
                            color = ThykraColors.MutedSlate
                        )
                    }
                    Spacer(Modifier.height(80.dp)) // FAB clearance
                }
            }
        }
    }

    pendingMemberAction?.let { action ->
        MemberActionConfirmDialog(
            action = action,
            onConfirm = {
                when (action.kind) {
                    MemberActionKind.REMOVE -> viewModel.removeMember(albumId, action.userId)
                    MemberActionKind.BLOCK -> viewModel.blockMember(albumId, action.userId)
                }
                pendingMemberAction = null
            },
            onDismiss = { pendingMemberAction = null }
        )
    }
}

private enum class MemberActionKind { REMOVE, BLOCK }

private data class PendingMemberAction(
    val userId: String,
    val displayName: String,
    val kind: MemberActionKind
)

/**
 * Confirmation dialog shown when an owner taps Remove or Block on a member
 * card. Both actions hit the server immediately on confirm — there is no
 * undo, but Block can be reversed from the Blocked-members section.
 */
@Composable
private fun MemberActionConfirmDialog(
    action: PendingMemberAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body, confirmLabel) = when (action.kind) {
        MemberActionKind.REMOVE -> Triple(
            "Remove member",
            "Remove ${action.displayName} from this album? They can rejoin if you share an invite link again.",
            "Remove"
        )
        MemberActionKind.BLOCK -> Triple(
            "Block member",
            "Block ${action.displayName}? They will be removed and can no longer rejoin via any invite link.",
            "Block"
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThykraColors.WarmWhite,
        title = {
            Text(text = title, color = ThykraColors.DeepNavy)
        },
        text = {
            Text(text = body, color = ThykraColors.MutedSlate)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (action.kind == MemberActionKind.BLOCK)
                        ThykraColors.SoftRed
                    else
                        ThykraColors.SkyBlue
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ThykraColors.MutedSlate
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Owner-only privacy card: shows the album's current visibility (private or
 * link-shared) and lets the owner flip between the two via a single button.
 * Mirrors the web app's privacy panel.
 */
@Composable
private fun VisibilitySection(
    currentVisibility: AlbumVisibility,
    onChangeVisibility: (AlbumVisibility) -> Unit
) {
    val isPrivate = currentVisibility == AlbumVisibility.PRIVATE
    val icon = if (isPrivate) ThykraIcons.Lock else ThykraIcons.Public
    val title = if (isPrivate) "Private" else "Anyone with the link"
    val description = if (isPrivate) {
        "Only added members can view this album."
    } else {
        "Anyone with the album link can view it."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = ThykraColors.SkyBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ThykraColors.SkyBlue
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ThykraColors.DeepNavy
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelMedium,
                        color = ThykraColors.MutedSlate,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onChangeVisibility(
                        if (isPrivate) AlbumVisibility.LINK_SHARED else AlbumVisibility.PRIVATE
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ThykraColors.SkyBlue
                )
            ) {
                Text(
                    text = if (isPrivate) "Make link-shared" else "Make private",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun publicAlbumUrl(albumId: String): String =
    "${com.jameeli.thykra.WEB_BASE_URL}/public/$albumId"

/** Configurable expiry options offered to the user when generating an invite link. */
private val INVITE_EXPIRY_OPTIONS = listOf(1, 7, 30, 90)

/**
 * Invite-link UI: lets the owner pick an expiry (1d / 7d / 30d / 90d) and tap
 * Generate to mint a new token. Once generated, shows the token and a copyable
 * deep-link URL.
 */
@Composable
private fun InviteLinkSection(
    inviteLink: com.jameeli.thykra.model.InviteLinkDto?,
    onGenerate: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(7) }
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThykraColors.Sandy),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Expires in",
                style = MaterialTheme.typography.labelMedium,
                color = ThykraColors.MutedSlate
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                INVITE_EXPIRY_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { selectedDays = days },
                        label = {
                            Text(
                                text = if (days == 1) "1 day" else "$days days",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ThykraColors.WarmWhite,
                            labelColor = ThykraColors.DeepNavy,
                            selectedContainerColor = ThykraColors.SkyBlue,
                            selectedLabelColor = Color.White,
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onGenerate(selectedDays) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThykraColors.SkyBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = ThykraIcons.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Generate invite link")
            }
            if (inviteLink != null) {
                Spacer(Modifier.height(10.dp))
                val url = "$API_BASE_URL/api/albums/join/${inviteLink.token}"
                Text(
                    text = "Invite link",
                    style = MaterialTheme.typography.labelMedium,
                    color = ThykraColors.MutedSlate
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThykraColors.WarmWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThykraColors.DeepNavy,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(url)) }
                        ) {
                            Icon(
                                imageVector = ThykraIcons.ContentCopy,
                                contentDescription = "Copy invite link",
                                tint = ThykraColors.SkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
