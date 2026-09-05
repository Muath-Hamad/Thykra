package com.jameeli.thykra.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.model.ActivityEventDto
import com.jameeli.thykra.model.ActivityEventType
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.ui.kit.ActivityCard
import com.jameeli.thykra.ui.kit.AvatarUser
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.ThykraButtonSpec
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.kit.skeleton
import com.jameeli.thykra.ui.kit.toAvatarUser
import com.jameeli.thykra.ui.social.ReactionEmoji
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraAnimate
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Design part 3 §10. What happened, in sentences.
 *
 * Events arrive pre-aggregated from the server — "Sara added 12 photos", not twelve rows
 * — so this screen only ever renders what it is given.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onOpenMedia: (albumId: String, mediaId: String) -> Unit,
    onOpenTrip: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Set on a trip's own feed: the trip name is already in the top bar. */
    tripTitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val events by viewModel.events.collectAsState()
    val lastSeenAt by viewModel.lastSeenAt.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val endOfList by viewModel.endOfList.collectAsState()
    val chrome = LocalThykraChrome.current

    LaunchedEffect(Unit) { viewModel.load() }

    // The dot follows the feed, and the marker is posted on the way out.
    LaunchedEffect(events, lastSeenAt) {
        if (tripTitle == null) chrome.activityDot = viewModel.hasUnseen
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.markSeen()
            if (tripTitle == null) chrome.activityDot = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (onBack != null) {
            TopAppBar(
                title = { Text(tripTitle.orEmpty(), style = MaterialTheme.typography.headlineSmall) },
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
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.load(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                loading && !loaded -> ActivitySkeletonList()

                events.isEmpty() && loaded -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        headline = clayPhrase("Quiet, ", "for now."),
                        body = "When friends add, react or comment, it shows here.",
                        glyph = EmptyGlyph.Person,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onBack == null) {
                        item("header") {
                            Text(
                                text = "What happened",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .semantics { heading() },
                            )
                        }
                    }

                    itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                        val unseen = lastSeenAt?.let { event.createdAt > it } ?: false
                        // The hairline that separates new from already-read.
                        val firstSeen = lastSeenAt != null &&
                            !unseen &&
                            events.getOrNull(index - 1)?.let { it.createdAt > lastSeenAt!! } == true
                        if (firstSeen) {
                            Text(
                                text = "Seen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.thykra.textMeta,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .semantics { heading() },
                            )
                        }
                        ActivityCard(
                            actor = event.actor.toAvatarUser(),
                            sentence = event.sentence(),
                            meta = event.meta(showTrip = tripTitle == null),
                            thumbnailUrls = event.thumbnailUrls,
                            unseen = unseen,
                            onClick = {
                                val mediaId = event.mediaIds.firstOrNull()
                                if (event.type == ActivityEventType.MEMBER_JOINED || mediaId == null) {
                                    onOpenTrip(event.album.id)
                                } else {
                                    onOpenMedia(event.album.id, mediaId)
                                }
                            },
                            modifier = Modifier.thykraAnimate(index = index),
                        )

                        if (index == events.lastIndex && !endOfList) {
                            LaunchedEffect(index) { viewModel.loadMore() }
                        }
                    }

                    if (endOfList && events.isNotEmpty()) {
                        item("end") {
                            Text(
                                text = "That's everything",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.thykra.textMeta,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Four rows, because that is what fits before the fold. */
@Composable
private fun ActivitySkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .skeleton(MaterialTheme.shapes.medium),
            )
        }
    }
}

/**
 * The sentence patterns from part 3 §10. The actor's name is prepended by the card, so
 * these start at the verb.
 */
private fun ActivityEventDto.sentence(): String = when (type) {
    ActivityEventType.MEDIA_ADDED -> {
        val n = count ?: mediaIds.size
        "added $n ${if (n == 1) "photo" else "photos"}"
    }

    ActivityEventType.REACTION -> {
        val n = count ?: 1
        val emoji = reactionType?.let { " ${ReactionEmoji.glyph(it)}" }.orEmpty()
        val targets = mediaIds.size.coerceAtLeast(1)
        "reacted$emoji to $targets ${if (targets == 1) "photo" else "photos"}"
    }

    ActivityEventType.COMMENT -> {
        val body = commentBody?.take(60)?.trim()
        if (body.isNullOrBlank()) "commented" else "commented “$body”"
    }

    ActivityEventType.MEMBER_JOINED -> "joined"
}

private fun ActivityEventDto.meta(showTrip: Boolean): String {
    val time = relativeTime(createdAt)
    return if (showTrip) "${album.title} · $time" else time
}

/** "2 h", "yesterday", "Tue", "9 Apr" — the shortest thing that is still unambiguous. */
private fun relativeTime(instant: Instant): String {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val minutes = ((nowMs - instant.toEpochMilliseconds()) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> "${minutes / 60} h"
        minutes < 2880 -> "yesterday"
        minutes < 10_080 -> "${minutes / 1440} days ago"
        else -> "${minutes / 1440} days ago"
    }
}
