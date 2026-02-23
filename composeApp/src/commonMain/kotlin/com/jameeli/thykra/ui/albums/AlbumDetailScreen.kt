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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.ui.media.rememberMediaPickerLauncher

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
    val inviteLink by viewModel.inviteLink.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val media by viewModel.media.collectAsState()
    val uploads by viewModel.uploads.collectAsState()

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
        topBar = {
            TopAppBar(
                title = { Text(album?.title ?: "Album") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("\u2190")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher() }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        if (isLoading && album == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Media section header
                item {
                    Text(
                        text = "Photos & Videos (${media.size})",
                        style = MaterialTheme.typography.titleMedium
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onNavigateToViewer(item.id) }
                                ) {
                                    AsyncImage(
                                        model = item.thumbnailUrl ?: item.url,
                                        contentDescription = item.filename,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
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
                            style = MaterialTheme.typography.titleMedium
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
                                Text(
                                    "\u2717",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = upload.filename,
                                    style = MaterialTheme.typography.bodySmall,
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
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(members) { member ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = member.role.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Invite link
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.createInviteLink(albumId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Invite Link")
                    }
                    if (inviteLink != null) {
                        Spacer(Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Invite Token:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = inviteLink!!.token,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp)) // FAB clearance
                }
            }
        }
    }
}
