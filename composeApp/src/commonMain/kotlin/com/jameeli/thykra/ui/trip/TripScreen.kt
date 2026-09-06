package com.jameeli.thykra.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.common_people_count
import com.jameeli.thykra.resources.common_photos_count
import com.jameeli.thykra.resources.trips_just_you
import com.jameeli.thykra.resources.common_try_again
import com.jameeli.thykra.resources.error_load_body
import com.jameeli.thykra.resources.trip_offline
import com.jameeli.thykra.resources.trip_select_all
import com.jameeli.thykra.resources.trip_selected_count
import com.jameeli.thykra.resources.trip_settings
import com.jameeli.thykra.resources.trip_videos_count
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.chapters.Chapter
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.navigation.ProvideBottomBar
import com.jameeli.thykra.ui.kit.AvatarSize
import com.jameeli.thykra.ui.kit.AvatarStack
import com.jameeli.thykra.ui.kit.ChapterHeader
import com.jameeli.thykra.ui.kit.ChapterSkeleton
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.OfflineBanner
import com.jameeli.thykra.ui.kit.PinnedChapterBar
import com.jameeli.thykra.ui.kit.Plate
import com.jameeli.thykra.ui.kit.Segmented
import com.jameeli.thykra.ui.kit.SegmentedOption
import com.jameeli.thykra.ui.kit.ThykraButtonSpec
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ToastTone
import com.jameeli.thykra.ui.kit.TripActionBar
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.media.rememberMediaPickerLauncher
import com.jameeli.thykra.ui.upload.UploadDockHost
import com.jameeli.thykra.ui.theme.HapticKind
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ScrimPillAlpha
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.rememberHaptics
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * Design part 3 §06. The trip, told by day.
 *
 * One staggered grid holds everything: the masthead, the segmented control and each
 * chapter header run full-span, and the plates fill the two columns at their own aspect.
 * That is what keeps a 300-photo trip on a single recycling list rather than a column of
 * nested ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    albumId: String,
    viewModel: TripViewModel,
    onBack: () -> Unit,
    onOpenViewer: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenRecaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val album by viewModel.album.collectAsState()
    val members by viewModel.members.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val media by viewModel.media.collectAsState()
    val uploads by viewModel.uploads.collectAsState()

    // Recomputed only when the in-flight set actually changes, not on every byte of
    // progress — the map decides layout, and re-laying out the grid at 30 Hz would be
    // both wasteful and visibly unsteady.
    val inFlight = uploads.inFlightFor(albumId)
    val pendingByDay = remember(inFlight.map { it.id }) { inFlight.pendingByDay() }
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val error by viewModel.error.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val connected by viewModel.connected.collectAsState()

    val chrome = LocalThykraChrome.current
    val haptic = rememberHaptics()
    var moreOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }

    val gridState = rememberLazyStaggeredGridState()
    val sheetState = rememberLazyGridState()

    val pickMedia = rememberMediaPickerLauncher { files ->
        if (files.isNotEmpty()) viewModel.uploadFiles(albumId, files)
    }

    LaunchedEffect(albumId) { viewModel.load(albumId) }

    // The dock is hosted by the shell, not by this screen, so it survives navigation.
    UploadDockHost(
        uploadQueueManager = viewModel.uploadQueueManager,
        tripTitleFor = { album?.title.orEmpty() },
        albumId = albumId,
        networkMonitor = viewModel.networkMonitorOrNull,
        onSeeThem = { viewModel.refreshMedia(albumId) },
        onBatchComplete = { viewModel.refreshMedia(albumId) },
    )

    LaunchedEffect(error) {
        val message = error
        if (message != null && album != null) {
            chrome.toast.show(message, ToastTone.Error)
            viewModel.clearError()
        }
    }

    // The masthead is item 0, so anything past it means the title belongs in the bar.
    val mastheadGone by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 }
    }

    // The chapter whose header has scrolled past — what the pinned bar shows.
    val pinnedChapter by remember(chapters, pendingByDay) {
        derivedStateOf {
            val firstVisible = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            chapters.lastOrNull { chapter ->
                // Placeholder plates occupy grid slots too, so the count has to include
                // them or the pinned bar names the wrong day mid-upload.
                val header = chapter.headerIndex(chapters, hasActivityStrip = false, pendingByDay = pendingByDay)
                (header ?: Int.MAX_VALUE) <= firstVisible
            }
        }
    }

    ProvideBottomBar {
        if (selection.isNotEmpty()) {
            SelectionActionBar(
                count = selection.size,
                canRemove = viewModel.canRemoveSelection(),
                onShare = { shareOpen = true },
                onRemove = {
                    viewModel.removeSelected(albumId) { removed ->
                        chrome.toast.show(
                            if (removed == 1) "Photo removed" else "$removed photos removed",
                        )
                    }
                },
            )
        } else {
            TripActionBar(
                role = viewModel.role ?: MemberRole.VIEWER,
                onAddPhotos = pickMedia,
                onShare = { shareOpen = true },
                onMore = { moreOpen = true },
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.load(albumId, refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                loading && !loaded -> TripSkeleton()

                album == null && loaded -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        headline = clayPhrase("Something ", "slipped."),
                        body = stringResource(Res.string.error_load_body),
                        glyph = if (connected) EmptyGlyph.Plate else EmptyGlyph.Offline,
                        primary = ThykraButtonSpec(
                            label = stringResource(Res.string.common_try_again),
                            onClick = { viewModel.load(albumId) },
                            variant = ThykraButtonVariant.Outlined,
                            icon = ThykraIcons.Retry,
                        ),
                    )
                }

                else -> Crossfade(
                    targetState = layout,
                    animationSpec = thykraTween(LocalMotion.current.dur2),
                    label = "tripLayout",
                ) { current ->
                    when (current) {
                        TripLayout.Days -> DaysGrid(
                            album = album,
                            members = members,
                            chapters = chapters,
                            pendingByDay = pendingByDay,
                            layout = layout,
                            selection = selection,
                            state = gridState,
                            isOwner = viewModel.role == MemberRole.OWNER,
                            canAdd = viewModel.role != MemberRole.VIEWER,
                            onSetLayout = viewModel::setLayout,
                            onOpenViewer = onOpenViewer,
                            onLongPressPlate = { id ->
                                haptic(HapticKind.LongPress)
                                viewModel.startSelection(id)
                            },
                            onToggleSelection = { id ->
                                haptic(HapticKind.Tick)
                                viewModel.toggleSelection(id)
                            },
                            onAddPhotos = pickMedia,
                            onShare = { shareOpen = true },
                            onBack = onBack,
                            onSettings = onOpenSettings,
                        )

                        TripLayout.Sheet -> ContactSheet(
                            media = media,
                            selection = selection,
                            state = sheetState,
                            layout = layout,
                            onSetLayout = viewModel::setLayout,
                            onOpenViewer = onOpenViewer,
                            onLongPressPlate = { id ->
                                haptic(HapticKind.LongPress)
                                viewModel.startSelection(id)
                            },
                            onToggleSelection = { id ->
                                haptic(HapticKind.Tick)
                                viewModel.toggleSelection(id)
                            },
                        )
                    }
                }
            }
        }

        // The top bar floats over the masthead and only becomes a bar once it has gone.
        TripTopBar(
            title = album?.title.orEmpty(),
            showTitle = mastheadGone,
            selectionCount = selection.size,
            onBack = { if (selection.isNotEmpty()) viewModel.clearSelection() else onBack() },
            onSettings = onOpenSettings,
            onSelectAll = viewModel::selectAll,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
        ) {
            OfflineBanner(visible = !connected, message = stringResource(Res.string.trip_offline))
            AnimatedVisibility(
                visible = mastheadGone && pinnedChapter != null && selection.isEmpty(),
                enter = fadeIn(thykraTween(LocalMotion.current.dur1)),
                exit = fadeOut(thykraTween(LocalMotion.current.dur1)),
            ) {
                pinnedChapter?.let { PinnedChapterBar(chapter = it) }
            }
        }
    }

    if (moreOpen) {
        TripMoreSheet(
            onDismiss = { moreOpen = false },
            onActivity = { moreOpen = false; onOpenActivity() },
            onRecaps = { moreOpen = false; onOpenRecaps() },
            onSelectPhotos = {
                moreOpen = false
                media.firstOrNull()?.let { viewModel.startSelection(it.id) }
            },
            onSettings = { moreOpen = false; onOpenSettings() },
        )
    }

    if (shareOpen) {
        val current = album
        if (current != null) {
            ShareTripSheet(
                album = current,
                isOwner = viewModel.role == MemberRole.OWNER,
                onDismiss = { shareOpen = false },
                onEnsureLink = { onReady -> viewModel.ensureShareLink(albumId, onReady) },
                onSetVisibility = { viewModel.setVisibility(albumId, it) },
                onCopied = { chrome.toast.show("Link copied", ToastTone.Success) },
            )
        }
    }
}

// ── Days ──────────────────────────────────────────────────────────────────────

@Composable
private fun DaysGrid(
    album: AlbumDto?,
    members: List<com.jameeli.thykra.model.AlbumMemberDto>,
    chapters: List<Chapter<MediaDto>>,
    /** In-flight uploads, keyed by the day their plate will land on. */
    pendingByDay: Map<kotlinx.datetime.LocalDate, List<com.jameeli.thykra.api.UploadState>>,
    layout: TripLayout,
    selection: Set<String>,
    state: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    isOwner: Boolean,
    canAdd: Boolean,
    onSetLayout: (TripLayout) -> Unit,
    onOpenViewer: (String) -> Unit,
    onLongPressPlate: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onAddPhotos: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val windowHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }
    // The lead plate is capped at 56% of the window — not 62% as on web, because the
    // bottom bar and status bar eat the difference.
    val leadCap = windowHeight * 0.56f

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = Modifier.fillMaxSize(),
        verticalItemSpacing = 3.dp,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "masthead", span = StaggeredGridItemSpan.FullLine) {
            TripMasthead(album = album, members = members)
        }

        item(key = "segmented", span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Segmented(
                    options = listOf(
                        SegmentedOption("Days", ThykraIcons.Chapters),
                        SegmentedOption("Sheet", ThykraIcons.Grid),
                    ),
                    selectedIndex = if (layout == TripLayout.Days) 0 else 1,
                    onSelect = { onSetLayout(if (it == 0) TripLayout.Days else TripLayout.Sheet) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (chapters.isEmpty()) {
            item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                EmptyTrip(isOwner = isOwner, canAdd = canAdd, onAddPhotos = onAddPhotos, onShare = onShare)
            }
        }

        // A photo from a day this trip has never seen has no chapter to sit in yet. Its
        // placeholders lead the grid until the upload confirms and the refreshed media
        // builds the real chapter for that day.
        val chapterDays = chapters.map { it.date }.toSet()
        val homeless = pendingByDay.filterKeys { it !in chapterDays }.values.flatten()
        items(
            items = homeless,
            key = { "pending-new:${it.id}" },
        ) { upload ->
            PendingPlate(upload = upload, modifier = Modifier.fillMaxWidth())
        }

        chapters.forEach { chapter ->
            item(key = "header:${chapter.key}", span = StaggeredGridItemSpan.FullLine) {
                ChapterHeader(
                    chapter = chapter,
                    contributors = chapter.contributors(members),
                )
            }

            val hero = chapter.heroCandidate
            item(key = "lead:${chapter.key}", span = StaggeredGridItemSpan.FullLine) {
                Plate(
                    media = hero,
                    // Centre-crop only when it is wider than 2:1; otherwise let it breathe.
                    contentScale = if ((hero.aspect() ?: 1f) > 2f) {
                        ContentScale.Crop
                    } else {
                        ContentScale.Fit
                    },
                    selected = hero.id in selection,
                    contentDescription = hero.describe(members),
                    onClick = {
                        if (selection.isEmpty()) onOpenViewer(hero.id) else onToggleSelection(hero.id)
                    },
                    onLongClick = { onLongPressPlate(hero.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = leadCap),
                )
            }

            // The day's arrivals sit at the head of its plates rather than the tail:
            // they are the newest thing in the chapter, and burying them under thirty
            // existing photos hides the only part that is changing.
            items(
                items = pendingByDay[chapter.date].orEmpty(),
                key = { "pending:${it.id}" },
            ) { upload ->
                PendingPlate(upload = upload, modifier = Modifier.fillMaxWidth())
            }

            items(
                items = chapter.items.filter { it.id != hero.id },
                key = { "plate:${it.id}" },
            ) { item ->
                Plate(
                    media = item,
                    selected = item.id in selection,
                    contentDescription = item.describe(members),
                    onClick = {
                        if (selection.isEmpty()) onOpenViewer(item.id) else onToggleSelection(item.id)
                    },
                    onLongClick = { onLongPressPlate(item.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * A `items` shim, because the staggered grid scope names it differently per version.
 *
 * Generic rather than `MediaDto`-only: the grid also lays out placeholder plates for
 * uploads that have not arrived yet, and those are not media.
 */
private fun <T> androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope.items(
    items: List<T>,
    key: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    items.forEach { item ->
        item(key = key(item)) { itemContent(item) }
    }
}

// ── Contact sheet ─────────────────────────────────────────────────────────────

@Composable
private fun ContactSheet(
    media: List<MediaDto>,
    selection: Set<String>,
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    layout: TripLayout,
    onSetLayout: (TripLayout) -> Unit,
    onOpenViewer: (String) -> Unit,
    onLongPressPlate: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = state,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(
            key = "segmented",
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Segmented(
                    options = listOf(
                        SegmentedOption("Days", ThykraIcons.Chapters),
                        SegmentedOption("Sheet", ThykraIcons.Grid),
                    ),
                    selectedIndex = if (layout == TripLayout.Days) 0 else 1,
                    onSelect = { onSetLayout(if (it == 0) TripLayout.Days else TripLayout.Sheet) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        itemsIndexed(media, key = { _, item -> item.id }) { _, item ->
            Plate(
                media = item,
                aspectRatio = 1f,
                selected = item.id in selection,
                onClick = {
                    if (selection.isEmpty()) onOpenViewer(item.id) else onToggleSelection(item.id)
                },
                onLongClick = { onLongPressPlate(item.id) },
            )
        }
    }
}

// ── Masthead and chrome ───────────────────────────────────────────────────────

@Composable
private fun TripMasthead(
    album: AlbumDto?,
    members: List<com.jameeli.thykra.model.AlbumMemberDto>,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            if (album?.coverUrl.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceVariant, PlateShape),
                )
            } else {
                AsyncImage(
                    model = album?.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Ink under the status bar so the Back and Settings pills always hold.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.38f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = album?.title.orEmpty(),
                style = MaterialTheme.typography.displayLarge,
                color = scheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AvatarStack(
                    users = members.map { it.toAvatarUser() },
                    max = 3,
                    size = AvatarSize.Xs,
                    ringColor = scheme.surface,
                    totalCount = album?.memberCount ?: members.size,
                )
                Text(
                    text = mastheadMeta(album),
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun mastheadMeta(album: AlbumDto?): String {
    if (album == null) return ""
    val parts = mutableListOf<String>()
    parts += if (album.memberCount == 1) {
        stringResource(Res.string.trips_just_you)
    } else {
        stringResource(Res.string.common_people_count, album.memberCount)
    }
    if (album.mediaCount > 0) parts += stringResource(Res.string.common_photos_count, album.mediaCount)
    if (album.videoCount > 0) parts += stringResource(Res.string.trip_videos_count, album.videoCount)
    return parts.joinToString(" · ")
}

@Composable
private fun TripTopBar(
    title: String,
    showTitle: Boolean,
    selectionCount: Int,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val selecting = selectionCount > 0
    val container by animateColorAsState(
        targetValue = when {
            selecting -> scheme.primaryContainer
            showTitle -> scheme.surface
            else -> Color.Transparent
        },
        animationSpec = thykraTween(LocalMotion.current.dur2),
        label = "tripTopBar",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChromeIcon(
            icon = if (selecting) ThykraIcons.Close else ThykraIcons.Back,
            contentDescription = if (selecting) "Cancel selection" else "Back",
            onClick = onBack,
            scrimmed = !showTitle && !selecting,
        )

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            when {
                selecting -> Text(
                    text = stringResource(Res.string.trip_selected_count, selectionCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimaryContainer,
                )

                showTitle -> Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (selecting) {
            Text(
                text = stringResource(Res.string.trip_select_all),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .androidxClickable(onSelectAll),
            )
        } else {
            ChromeIcon(
                icon = ThykraIcons.Settings,
                contentDescription = stringResource(Res.string.trip_settings),
                onClick = onSettings,
                scrimmed = !showTitle,
            )
        }
    }
}

/** A pill over the photograph, or a plain icon once the bar is opaque. */
@Composable
private fun ChromeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    scrimmed: Boolean,
) {
    val extended = MaterialTheme.thykra
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(
                if (scrimmed) {
                    Modifier.background(
                        extended.scrimStrong.copy(alpha = ScrimPillAlpha),
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .androidxClickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (scrimmed) extended.onScrim else scheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun Modifier.androidxClickable(onClick: () -> Unit): Modifier =
    this.clickable(role = Role.Button, onClick = onClick)

@Composable
private fun TripSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        ChapterSkeleton()
    }
}

@Composable
private fun EmptyTrip(
    isOwner: Boolean,
    canAdd: Boolean,
    onAddPhotos: () -> Unit,
    onShare: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            headline = clayPhrase("Nothing here ", "yet."),
            body = if (isOwner) {
                "Add the first photos and the trip starts telling itself by day. " +
                    "Then invite the people who were there."
            } else {
                "Add the first photos and the trip starts telling itself by day."
            },
            glyph = EmptyGlyph.Plate,
            primary = if (canAdd) {
                ThykraButtonSpec("Add photos", onAddPhotos, icon = ThykraIcons.Plus)
            } else {
                null
            },
            secondary = if (isOwner) {
                ThykraButtonSpec(
                    "Invite friends",
                    onShare,
                    variant = ThykraButtonVariant.People,
                    icon = ThykraIcons.PersonAdd,
                )
            } else {
                null
            },
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun MediaDto.aspect(): Float? {
    val w = width ?: return null
    val h = height ?: return null
    if (w <= 0 || h <= 0) return null
    return w.toFloat() / h.toFloat()
}

/** "Photo by Sara, Saturday 12 April" — never the filename. */
private fun MediaDto.describe(members: List<com.jameeli.thykra.model.AlbumMemberDto>): String {
    val who = members.firstOrNull { it.userId == uploaderId }?.displayName
    val kind = if (type == com.jameeli.thykra.model.MediaType.VIDEO) "Video" else "Photo"
    return if (who != null) "$kind by $who" else kind
}

private fun Chapter<MediaDto>.contributors(
    members: List<com.jameeli.thykra.model.AlbumMemberDto>,
) = items
    .map { it.uploaderId }
    .distinct()
    .mapNotNull { id -> members.firstOrNull { it.userId == id } }
    .map { it.toAvatarUser() }

/**
 * Where this chapter's header sits in the grid, counting the fixed items above it.
 * Used only to decide which chapter the pinned bar names.
 */
private fun Chapter<MediaDto>.headerIndex(
    chapters: List<Chapter<MediaDto>>,
    hasActivityStrip: Boolean,
    pendingByDay: Map<kotlinx.datetime.LocalDate, List<com.jameeli.thykra.api.UploadState>> = emptyMap(),
): Int? {
    val chapterDays = chapters.map { it.date }.toSet()
    // Placeholders for days the trip has no chapter for yet lead the whole grid, so they
    // push every header down.
    val homeless = pendingByDay.filterKeys { it !in chapterDays }.values.sumOf { it.size }
    var index = (if (hasActivityStrip) 3 else 2) + homeless
    for (chapter in chapters) {
        if (chapter.key == key) return index
        // header + lead + this day's placeholders + the rest of the day's plates
        index += 2 + pendingByDay[chapter.date].orEmpty().size + (chapter.items.size - 1)
    }
    return null
}
