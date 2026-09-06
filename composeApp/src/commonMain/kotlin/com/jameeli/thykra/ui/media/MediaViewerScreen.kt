package com.jameeli.thykra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jameeli.thykra.API_BASE_URL
import com.jameeli.thykra.chapters.Chapter
import com.jameeli.thykra.chapters.chapterOfMedia
import com.jameeli.thykra.chapters.groupIntoChapters
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.ui.kit.ViewerChrome
import com.jameeli.thykra.ui.kit.formatChapterDate
import com.jameeli.thykra.ui.social.CommentsSheet
import com.jameeli.thykra.ui.social.MediaCommentsViewModel
import com.jameeli.thykra.ui.social.MediaReactionsViewModel
import com.jameeli.thykra.ui.social.ReactionPicker as EmojiReactionPicker
import com.jameeli.thykra.ui.theme.LightStatusBarIcons
import com.jameeli.thykra.ui.theme.ThykraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The chrome takes itself away after this much stillness. */
private const val ChromeIdleMs = 2_500L

/**
 * Design part 3 §07.
 *
 * Always Darkroom, whatever the preference says — it is the one place the two themes
 * converge, because a photograph should be looked at against a dark ground and nothing
 * else on the screen matters while you are looking at it.
 */
@Composable
fun MediaViewerScreenContent(
    albumId: String,
    initialMediaId: String,
    viewModel: MediaViewerViewModel,
    reactionsViewModelFactory: () -> MediaReactionsViewModel,
    commentsViewModelFactory: () -> MediaCommentsViewModel,
    onNavigateBack: () -> Unit,
) {
    ThykraTheme(forceDark = true) {
        LightStatusBarIcons(enabled = true)
        ViewerBody(
            albumId = albumId,
            initialMediaId = initialMediaId,
            viewModel = viewModel,
            reactionsViewModelFactory = reactionsViewModelFactory,
            commentsViewModelFactory = commentsViewModelFactory,
            onNavigateBack = onNavigateBack,
        )
    }
}

@Composable
private fun ViewerBody(
    albumId: String,
    initialMediaId: String,
    viewModel: MediaViewerViewModel,
    reactionsViewModelFactory: () -> MediaReactionsViewModel,
    commentsViewModelFactory: () -> MediaCommentsViewModel,
    onNavigateBack: () -> Unit,
) {
    val media by viewModel.media.collectAsState()
    val album by viewModel.album.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    LaunchedEffect(albumId) { viewModel.loadMedia(albumId) }

    val initialPage = remember(media, initialMediaId) {
        media.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = 0) { media.size }
    LaunchedEffect(initialPage, media.size) {
        if (media.isNotEmpty()) pagerState.scrollToPage(initialPage)
    }

    val currentMedia = media.getOrNull(pagerState.currentPage)
    val reactionsVm = remember { reactionsViewModelFactory() }
    val commentsVm = remember { commentsViewModelFactory() }

    var commentsVisible by remember { mutableStateOf(false) }
    var infoVisible by remember { mutableStateOf(false) }
    var pickerVisible by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }

    val isLandscape = rememberIsLandscape()
    val filmstripState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Grouped so the position pill can say "14 / 31 · Sat 12 Apr" — the index within the
    // chapter, not within the whole trip, because the chapter is what you are looking at.
    val chapters = remember(media) { groupIntoChapters(media) }
    val chapter = remember(chapters, currentMedia?.id) {
        currentMedia?.id?.let { chapters.chapterOfMedia(it) }
    }

    LaunchedEffect(albumId, currentMedia?.id) {
        currentMedia?.id?.let { reactionsVm.load(albumId, it) }
    }
    LaunchedEffect(albumId, currentMedia?.id) {
        currentMedia?.id?.let { commentsVm.load(albumId, it) }
    }

    // Idle, and the chrome goes. Any of these bring it back or hold it.
    LaunchedEffect(chromeVisible, pagerState.currentPage, commentsVisible, infoVisible) {
        if (!chromeVisible || commentsVisible || infoVisible) return@LaunchedEffect
        delay(ChromeIdleMs)
        chromeVisible = false
    }
    // Zooming hides it outright — the photograph is the point.
    LaunchedEffect(zoomed) {
        if (zoomed) chromeVisible = false
    }

    // Keep the filmstrip's current thumb centred as the pager moves.
    LaunchedEffect(pagerState.currentPage) {
        if (media.isNotEmpty()) {
            filmstripState.animateScrollToItem(
                index = pagerState.currentPage.coerceAtLeast(0),
                scrollOffset = -120,
            )
        }
    }

    val reactions by reactionsVm.reactions.collectAsState()
    val comments by commentsVm.comments.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        if (media.isEmpty()) {
            Text(
                text = "Loading…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Off-screen pages never initialise a decoder they will not use.
                beyondViewportPageCount = 1,
                userScrollEnabled = !zoomed,
            ) { page ->
                val item = media[page]
                val resolvedUrl = item.url.replace("http://localhost:8081", API_BASE_URL)
                if (item.type == MediaType.VIDEO) {
                    VideoPlayer(
                        url = resolvedUrl,
                        isActive = page == pagerState.currentPage,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ZoomableAsyncImage(
                        url = resolvedUrl,
                        contentDescription = item.describe(),
                        onZoomChanged = { zoomed = it },
                        onTap = { chromeVisible = !chromeVisible },
                        onLongPress = { pickerVisible = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        ViewerChrome(
            positionLabel = positionLabel(chapter, currentMedia),
            reactions = reactions?.summary.orEmpty(),
            commentCount = comments.size,
            visible = chromeVisible,
            onBack = onNavigateBack,
            onInfo = { infoVisible = true },
            onMore = { infoVisible = true },
            onToggleReaction = { type ->
                currentMedia?.let { reactionsVm.toggle(albumId, it.id, type) }
            },
            onOpenPicker = { pickerVisible = true },
            onComments = { commentsVisible = true },
            attribution = currentMedia?.let { item ->
                attributionFor(
                    name = album?.previewMembers
                        ?.firstOrNull { it.userId == item.uploaderId }
                        ?.displayName,
                    takenAt = item.takenAt,
                )
            },
            filmstrip = media,
            currentIndex = pagerState.currentPage,
            onSelectIndex = { index ->
                chromeVisible = true
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            landscape = isLandscape,
            filmstripState = filmstripState,
        )

        if (pickerVisible && currentMedia != null) {
            EmojiReactionPicker(
                reactions = reactions,
                onToggle = { type ->
                    reactionsVm.toggle(albumId, currentMedia.id, type)
                    pickerVisible = false
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .semantics { contentDescription = "Pick a reaction" },
            )
        }

        if (currentMedia != null) {
            CommentsSheet(
                visible = commentsVisible,
                albumId = albumId,
                mediaId = currentMedia.id,
                currentUserId = currentUserId.orEmpty(),
                isAlbumOwner = currentUserId != null && album?.ownerId == currentUserId,
                viewModel = commentsVm,
                onDismiss = { commentsVisible = false },
            )

            if (infoVisible) {
                MediaInfoSheet(
                    media = currentMedia,
                    uploaderName = album?.previewMembers
                        ?.firstOrNull { it.userId == currentMedia.uploaderId }
                        ?.displayName,
                    onDismiss = { infoVisible = false },
                )
            }
        }
    }
}

/** "14 / 31 · Sat 12 Apr" — the index within the chapter, and the chapter's date. */
private fun positionLabel(chapter: Chapter<MediaDto>?, current: MediaDto?): String {
    if (chapter == null || current == null) return ""
    val index = chapter.items.indexOfFirst { it.id == current.id } + 1
    val date = if (chapter.dated) formatChapterDate(chapter.date) else "Added later"
    return "$index / ${chapter.items.size} · $date"
}

private fun MediaDto.describe(): String =
    if (type == MediaType.VIDEO) "Video" else "Photo"

/** "Sara Nasser · 4:12 pm", or just the time when we do not know who added it. */
private fun attributionFor(name: String?, takenAt: Instant?): String? {
    val time = takenAt?.let { instant ->
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour12 = if (local.hour % 12 == 0) 12 else local.hour % 12
        val suffix = if (local.hour < 12) "am" else "pm"
        "$hour12:${local.minute.toString().padStart(2, '0')} $suffix"
    }
    return listOfNotNull(name, time).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/**
 * Pinch to 4x, double-tap to 2.5x, pan when zoomed. Single tap toggles the chrome and
 * long-press opens the reaction picker at the touch point, so the two gestures a
 * photograph invites both land somewhere useful.
 */
@Composable
private fun ZoomableAsyncImage(
    url: String,
    contentDescription: String?,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offsetX by remember(url) { mutableStateOf(0f) }
    var offsetY by remember(url) { mutableStateOf(0f) }

    LaunchedEffect(scale) { onZoomChanged(scale > 1.01f) }

    Box(
        modifier = modifier
            .pointerInput(url) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1.01f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
        )
    }
}
