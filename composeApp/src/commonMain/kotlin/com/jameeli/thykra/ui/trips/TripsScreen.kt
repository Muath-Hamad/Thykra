package com.jameeli.thykra.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.api.NetworkMonitor
import com.jameeli.thykra.api.UploadQueueManager
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.OfflineBanner
import com.jameeli.thykra.ui.kit.ThykraButtonSpec
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ToastTone
import com.jameeli.thykra.ui.kit.TripCard
import com.jameeli.thykra.ui.kit.TripCardSkeleton
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.upload.UploadDockHost
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraAnimate
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Design part 3 §08. The Trips root.
 *
 * The top bar stays quiet: one action, and a title that only appears once the header has
 * scrolled away. Everything a thumb reaches for is at the bottom — the tab bar, and the
 * dock when an upload is live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: TripsViewModel,
    onOpenTrip: (String) -> Unit,
    uploadQueueManager: UploadQueueManager,
    modifier: Modifier = Modifier,
    networkMonitor: NetworkMonitor? = null,
) {
    val trips by viewModel.trips.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val error by viewModel.error.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val chrome = LocalThykraChrome.current

    var createOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // The pinned title arrives only once the big one has gone.
    val showPinnedTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.loadProfile()
    }

    // The list variant names the trip, because from here it is not obvious which one.
    UploadDockHost(
        uploadQueueManager = uploadQueueManager,
        tripTitleFor = { id -> trips.firstOrNull { it.id == id }?.title.orEmpty() },
        networkMonitor = networkMonitor,
        onSeeThem = onOpenTrip,
        onBatchComplete = { viewModel.load(refresh = true) },
    )

    // The Me tab draws the signed-in person rather than a glyph.
    LaunchedEffect(profile) {
        chrome.currentUser = profile?.toAvatarUser()
    }

    // A failure with a cached list is a Toast; a failure with nothing cached is the
    // empty state below. Never both.
    LaunchedEffect(error, trips.isEmpty()) {
        val message = error
        if (message != null && trips.isNotEmpty()) {
            chrome.toast.show(message, ToastTone.Error)
            viewModel.clearError()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (showPinnedTitle) {
                    Text("Trips", style = MaterialTheme.typography.headlineSmall)
                }
            },
            actions = {
                IconButton(onClick = { createOpen = true }) {
                    Icon(ThykraIcons.Plus, contentDescription = "New trip")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        OfflineBanner(visible = !connected)

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                loading && !loaded -> LoadingList()

                trips.isEmpty() && loaded && error == null -> EmptyTrips(
                    firstName = profile?.displayName?.substringBefore(' '),
                    onStart = { createOpen = true },
                )

                trips.isEmpty() && loaded -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        headline = clayPhrase("Something ", "slipped."),
                        body = "We couldn't load this. Try again in a moment.",
                        glyph = if (connected) EmptyGlyph.Plate else EmptyGlyph.Offline,
                        primary = ThykraButtonSpec(
                            label = "Try again",
                            onClick = { viewModel.load() },
                            variant = ThykraButtonVariant.Outlined,
                            icon = ThykraIcons.Retry,
                        ),
                    )
                }

                else -> TripList(
                    trips = trips,
                    greeting = greetingFor(profile?.displayName, trips),
                    meta = summaryOf(trips),
                    listState = listState,
                    onOpenTrip = onOpenTrip,
                )
            }
        }
    }

    if (createOpen) {
        CreateTripSheet(
            viewModel = viewModel,
            onDismiss = { createOpen = false },
            onCreated = { album ->
                createOpen = false
                onOpenTrip(album.id)
            },
        )
    }
}

@Composable
private fun TripList(
    trips: List<AlbumDto>,
    greeting: String,
    meta: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpenTrip: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.thykra.textMeta,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Cards rise and fade in, staggered for the first eight and no further.
        itemsIndexed(trips, key = { _, album -> album.id }) { index, album ->
            TripCard(
                album = album,
                meta = tripMeta(album),
                videoCount = album.videoCount,
                onClick = { onOpenTrip(album.id) },
                modifier = Modifier.thykraAnimate(index = index),
            )
        }
    }
}

@Composable
private fun LoadingList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Two, because that is what fits above the fold and more would be a wall.
        repeat(2) { TripCardSkeleton() }
    }
}

@Composable
private fun EmptyTrips(firstName: String?, onStart: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            headline = clayPhrase("No trips ", "yet."),
            body = "Start one and invite the people who were there. " +
                "Or open an invite link someone sent you.",
            glyph = EmptyGlyph.Plate,
            primary = ThykraButtonSpec(
                label = "Start a trip",
                onClick = onStart,
                icon = ThykraIcons.Plus,
            ),
        )
    }
}

/** "Good to see you, Maya." on a first run, "Your trips" once there are any. */
private fun greetingFor(displayName: String?, trips: List<AlbumDto>): String {
    val first = displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    return if (trips.isEmpty() && first != null) "Good to see you, $first." else "Your trips"
}

/** "3 trips · 412 photos · 11 people", summed over the list. */
private fun summaryOf(trips: List<AlbumDto>): String {
    val photos = trips.sumOf { it.mediaCount }
    val people = trips.sumOf { it.memberCount }
    return listOf(
        plural(trips.size, "trip"),
        plural(photos, "photo"),
        plural(people, "person", "people"),
    ).joinToString(" · ")
}

/** "84 photos · 6 people · Apr 2026" for one card. */
private fun tripMeta(album: AlbumDto): String {
    val parts = mutableListOf<String>()
    if (album.mediaCount > 0) parts += plural(album.mediaCount, "photo")
    parts += if (album.memberCount <= 1) "just you" else plural(album.memberCount, "person", "people")
    parts += monthYearOf(album.lastActivityAt ?: album.createdAt)
    return parts.joinToString(" · ")
}

private fun plural(count: Int, singular: String, plural: String = "${singular}s") =
    "$count ${if (count == 1) singular else plural}"

private val MonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun monthYearOf(instant: Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${MonthNames[date.monthNumber - 1]} ${date.year}"
}
