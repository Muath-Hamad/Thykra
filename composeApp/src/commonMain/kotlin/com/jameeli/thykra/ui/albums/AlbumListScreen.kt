package com.jameeli.thykra.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.api.UploadStatus
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberSummary
import com.jameeli.thykra.ui.theme.ThykraColors
import com.jameeli.thykra.ui.theme.ThykraIcons
import kotlin.math.abs

private val avatarPalette = listOf(
    ThykraColors.SkyBlue,
    ThykraColors.SunriseOrange,
    Color(0xFF7B5EA7),
    ThykraColors.OceanBlue,
    Color(0xFF3A9E6F),
)

private fun avatarColor(userId: String): Color =
    avatarPalette[abs(userId.hashCode()) % avatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreenContent(
    viewModel: AlbumListViewModel,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    uploadQueueManager: UploadQueueManager? = null
) {
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val defaultFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.jameeli.thykra.api.UploadState>()) }
    val uploadStates by (uploadQueueManager?.uploads ?: defaultFlow).collectAsState()
    val activeUploadCount = uploadStates.count {
        it.status == UploadStatus.QUEUED || it.status == UploadStatus.UPLOADING || it.status == UploadStatus.CONFIRMING
    }

    LaunchedEffect(Unit) {
        viewModel.loadAlbums()
    }

    Scaffold(
        containerColor = ThykraColors.WarmWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thykra",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (activeUploadCount > 0) {
                        Box(
                            modifier = Modifier.size(40.dp).padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = ThykraColors.SkyBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "$activeUploadCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThykraColors.SkyBlue
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = ThykraIcons.Person,
                            contentDescription = "Profile",
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
                onClick = { showCreateDialog = true },
                containerColor = ThykraColors.SkyBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = ThykraIcons.Add,
                    contentDescription = "Create Album"
                )
            }
        }
    ) { padding ->
        if (isLoading && albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ThykraColors.SkyBlue)
            }
        } else if (error != null && albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThykraColors.SoftRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadAlbums() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThykraColors.SkyBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = ThykraIcons.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ThykraColors.MutedSlate.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No albums yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThykraColors.DeepNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Create your first album to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThykraColors.MutedSlate
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThykraColors.SkyBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Album")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onNavigateToAlbum(album.id) })
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description ->
                viewModel.createAlbum(title, description) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun AlbumCard(album: AlbumDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (album.coverUrl != null) {
            AsyncImage(
                model = album.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ThykraColors.OceanBlue.copy(alpha = 0.55f),
                                ThykraColors.SkyBlue
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = album.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        // Permanent bottom vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f to ThykraColors.DeepNavy.copy(alpha = 0.7f)
                    )
                )
        )

        // Info panel — always visible (mobile)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarStack(members = album.previewMembers, totalCount = album.memberCount)
                Text(
                    text = "${album.memberCount} member${if (album.memberCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AvatarStack(
    members: List<AlbumMemberSummary>,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val display = members.take(3)
    val extra = totalCount - display.size

    Layout(
        modifier = modifier,
        content = {
            display.forEach { member ->
                AvatarCircle(
                    initial = member.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = avatarColor(member.userId)
                )
            }
            if (extra > 0) {
                AvatarCircle(initial = "+$extra", color = ThykraColors.MutedSlate)
            }
        }
    ) { measurables, _ ->
        val avatarPx = 22.dp.roundToPx()
        val overlapPx = 6.dp.roundToPx()
        val step = avatarPx - overlapPx
        val itemConstraints = Constraints.fixed(avatarPx, avatarPx)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val totalWidth = if (placeables.isEmpty()) 0 else avatarPx + (placeables.size - 1) * step
        layout(totalWidth, avatarPx) {
            // Reverse iteration so the first (leftmost) avatar paints on top
            for (i in placeables.indices.reversed()) {
                placeables[i].placeRelative(i * step, 0)
            }
        }
    }
}

@Composable
private fun AvatarCircle(initial: String, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.5.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun CreateAlbumDialog(onDismiss: () -> Unit, onCreate: (String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThykraColors.WarmWhite,
        title = {
            Text(
                "Create Album",
                style = MaterialTheme.typography.titleLarge,
                color = ThykraColors.DeepNavy
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThykraColors.SkyBlue,
                        focusedLabelColor = ThykraColors.SkyBlue,
                        cursorColor = ThykraColors.SkyBlue
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThykraColors.SkyBlue,
                        focusedLabelColor = ThykraColors.SkyBlue,
                        cursorColor = ThykraColors.SkyBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description.ifBlank { null }) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThykraColors.SkyBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ThykraColors.MutedSlate)
            }
        }
    )
}
