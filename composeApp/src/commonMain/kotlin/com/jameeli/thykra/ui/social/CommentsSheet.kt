package com.jameeli.thykra.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.model.CommentDto
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.ThykraIcons

/**
 * Modal-style comments panel anchored to the bottom of the screen. Implemented as a
 * plain Box overlay so it works identically on Android + iOS (no Material3 ModalBottomSheet
 * dependency, which has had iOS layout quirks in Compose Multiplatform).
 *
 * The caller is responsible for showing/hiding via [visible]. When dismissed (tap on scrim)
 * [onDismiss] is invoked.
 */
@Composable
fun CommentsSheet(
    visible: Boolean,
    albumId: String,
    mediaId: String,
    currentUserId: String,
    isAlbumOwner: Boolean,
    viewModel: MediaCommentsViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(albumId, mediaId) {
        viewModel.load(albumId, mediaId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss)
        )

        // Sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(ThykraColors.WarmWhite)
                .clickable(enabled = false, onClick = {}) // swallow clicks so scrim doesn't dismiss
                .imePadding()
        ) {
            // Drag handle + header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ThykraColors.MutedSlate.copy(alpha = 0.4f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThykraColors.DeepNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = comments.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ThykraColors.MutedSlate
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = ThykraIcons.Close,
                        contentDescription = "Close",
                        tint = ThykraColors.MutedSlate
                    )
                }
            }

            HorizontalDivider(color = ThykraColors.MutedSlate.copy(alpha = 0.15f))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && comments.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ThykraColors.SkyBlue)
                        }
                    }
                    comments.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No comments yet — be the first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThykraColors.MutedSlate
                            )
                        }
                    }
                    else -> {
                        val listState = rememberLazyListState()
                        // Auto-scroll to bottom when a new comment is added.
                        LaunchedEffect(comments.size) {
                            if (comments.isNotEmpty()) {
                                listState.animateScrollToItem(comments.size - 1)
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = comments, key = { it.id }) { comment ->
                                CommentRow(
                                    comment = comment,
                                    canEdit = comment.authorId == currentUserId,
                                    canDelete = comment.authorId == currentUserId || isAlbumOwner,
                                    onEdit = { newBody ->
                                        viewModel.update(albumId, mediaId, comment.id, newBody)
                                    },
                                    onDelete = {
                                        viewModel.delete(albumId, mediaId, comment.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            error?.let { msg ->
                Text(
                    text = msg,
                    color = ThykraColors.SoftRed,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            CommentInputBar(
                isSending = isSending,
                onSend = { body ->
                    viewModel.add(albumId, mediaId, body)
                },
                modifier = Modifier
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentDto,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var draftBody by remember(comment.id, comment.body) { mutableStateOf(comment.body) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(url = comment.authorAvatarUrl, name = comment.authorDisplayName)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (editing) {
                OutlinedTextField(
                    value = draftBody,
                    onValueChange = { draftBody = it.take(2000) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThykraColors.SkyBlue,
                        unfocusedBorderColor = ThykraColors.MutedSlate.copy(alpha = 0.3f),
                        cursorColor = ThykraColors.SkyBlue
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        editing = false
                        draftBody = comment.body
                    }) {
                        Text("Cancel", color = ThykraColors.MutedSlate)
                    }
                    TextButton(
                        onClick = {
                            val trimmed = draftBody.trim()
                            if (trimmed.isNotEmpty() && trimmed != comment.body) {
                                onEdit(trimmed)
                            }
                            editing = false
                        }
                    ) {
                        Text("Save", color = ThykraColors.SkyBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                val text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = ThykraColors.DeepNavy
                        )
                    ) {
                        append(comment.authorDisplayName)
                    }
                    append("  ")
                    withStyle(SpanStyle(color = ThykraColors.DeepNavy.copy(alpha = 0.85f))) {
                        append(comment.body)
                    }
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                if (comment.updatedAt != comment.createdAt) {
                    Text(
                        text = "edited",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThykraColors.MutedSlate.copy(alpha = 0.8f)
                    )
                }
            }
        }
        if ((canEdit || canDelete) && !editing) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    // Three small dots — render via vertical character to avoid bundling another icon.
                    Text(
                        text = "⋯",
                        color = ThykraColors.MutedSlate,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (canEdit) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                editing = true
                            }
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = ThykraColors.SoftRed) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(url: String?, name: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ThykraColors.SkyBlue.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            val resolved = url.replace("http://localhost:8081", API_BASE_URL)
            AsyncImage(
                model = resolved,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = ThykraColors.SkyBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CommentInputBar(
    isSending: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = ThykraColors.MutedSlate.copy(alpha = 0.15f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 2000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment…", color = ThykraColors.MutedSlate.copy(alpha = 0.7f)) },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThykraColors.SkyBlue,
                    unfocusedBorderColor = ThykraColors.MutedSlate.copy(alpha = 0.3f),
                    cursorColor = ThykraColors.SkyBlue
                )
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.trim().isNotEmpty() && !isSending
            IconButton(
                onClick = {
                    if (canSend) {
                        onSend(draft)
                        draft = ""
                    }
                },
                enabled = canSend
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ThykraColors.SkyBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "→",
                        color = if (canSend) ThykraColors.SkyBlue else ThykraColors.MutedSlate.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
