package com.jameeli.thykra.ui.recaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.recaps_build
import com.jameeli.thykra.resources.recaps_empty_body
import com.jameeli.thykra.resources.recaps_empty_head
import com.jameeli.thykra.resources.recaps_empty_tail
import com.jameeli.thykra.resources.recaps_eyebrow
import com.jameeli.thykra.resources.recaps_intro
import com.jameeli.thykra.resources.recaps_title
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.RecapDto
import com.jameeli.thykra.model.RecapStatus
import com.jameeli.thykra.navigation.LocalThykraChrome
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.RecapCard
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonSize
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ToastTone
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.kit.skeleton
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraAnimate

/**
 * Design part 3 §10. Short photo stories, built from a trip.
 *
 * Under the recaps come the trips that do not have one: eligible ones offer Build, and
 * the rest say how many more photographs they need rather than offering a button that
 * would fail.
 */
@Composable
fun RecapsScreen(
    viewModel: RecapsViewModel,
    onOpenRecap: (String) -> Unit,
    modifier: Modifier = Modifier,
    albumId: String? = null,
) {
    val recaps by viewModel.recaps.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val building by viewModel.building.collectAsState()
    val message by viewModel.message.collectAsState()
    val chrome = LocalThykraChrome.current

    LaunchedEffect(albumId) { viewModel.load(albumId) }

    LaunchedEffect(message) {
        message?.let {
            chrome.toast.show(it, ToastTone.Error)
            viewModel.consumeMessage()
        }
    }

    val without = viewModel.tripsWithoutRecap()

    if (loading && !loaded) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(2) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .skeleton(MaterialTheme.shapes.medium),
                )
            }
        }
        return
    }

    if (recaps.isEmpty() && without.isEmpty() && loaded) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                headline = clayPhrase(
                    stringResource(Res.string.recaps_empty_head),
                    stringResource(Res.string.recaps_empty_tail),
                ),
                body = stringResource(Res.string.recaps_empty_body, RecapMinimumPhotos),
                glyph = EmptyGlyph.Plate,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (albumId == null) {
            item("header") {
                Column {
                    Text(
                        text = stringResource(Res.string.recaps_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(Res.string.recaps_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.thykra.textMeta,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        items(recaps, key = { it.id }) { recap ->
            if (recap.status == RecapStatus.BUILDING) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .skeleton(MaterialTheme.shapes.medium),
                )
            } else {
                RecapCard(
                    title = recap.title,
                    eyebrow = stringResource(Res.string.recaps_eyebrow, recap.mediaIds.size),
                    coverUrl = recap.coverUrl,
                    onClick = { recap.shareToken?.let(onOpenRecap) },
                    modifier = Modifier.thykraAnimate(),
                )
            }
        }

        if (without.isNotEmpty()) {
            items(without, key = { "trip:${it.id}" }) { trip ->
                BuildRow(
                    trip = trip,
                    building = trip.id in building,
                    onBuild = { viewModel.build(trip.id) },
                )
            }
        }
    }
}

/**
 * A trip with no recap. Eligible ones get a tonal Build; the rest say what they are short
 * of, at 70% — a button that would fail is worse than a sentence that explains.
 */
@Composable
private fun BuildRow(
    trip: AlbumDto,
    building: Boolean,
    onBuild: () -> Unit,
) {
    val eligible = trip.mediaCount >= RecapMinimumPhotos
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (eligible) 1f else 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (eligible) {
                    "${trip.mediaCount} photos · no recap yet"
                } else {
                    "${trip.mediaCount} photos · needs $RecapMinimumPhotos to build"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.thykra.textMeta,
            )
        }
        if (eligible) {
            ThykraButton(
                label = stringResource(Res.string.recaps_build),
                onClick = onBuild,
                variant = ThykraButtonVariant.Tonal,
                size = ThykraButtonSize.Compact,
                loading = building,
            )
        }
    }
}

/** Recaps that are still generating. Used by the trip screen's own recap list. */
internal fun List<RecapDto>.building(): List<RecapDto> =
    filter { it.status == RecapStatus.BUILDING }
