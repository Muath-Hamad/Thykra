package com.jameeli.thykra.ui.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.common_people_count
import com.jameeli.thykra.resources.common_photos_count
import com.jameeli.thykra.resources.share_cancel
import com.jameeli.thykra.resources.share_new_trip
import com.jameeli.thykra.resources.share_no_trips
import com.jameeli.thykra.resources.share_pick_trip_many
import com.jameeli.thykra.resources.share_pick_trip_one
import com.jameeli.thykra.resources.share_pick_trip_title
import com.jameeli.thykra.ui.kit.SheetAction
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.skeleton
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import org.jetbrains.compose.resources.stringResource

/**
 * Where photos shared in from another app are aimed.
 *
 * A sheet rather than a screen: the person is mid-task in the gallery and expects to be
 * handed back, so this is one decision — which trip — and nothing else. Creating a trip
 * is offered because arriving with photos and no trip to put them in is a real first-run
 * case, and sending someone away empty-handed to make one loses the photos.
 *
 * The trips are listed newest-activity first, which is the same order the Trips tab uses,
 * so the one someone is most likely to want is under their thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePickerSheet(
    fileCount: Int,
    trips: List<AlbumDto>,
    loading: Boolean,
    onPick: (AlbumDto) -> Unit,
    onCreateTrip: () -> Unit,
    onDismiss: () -> Unit,
) {
    ThykraSheet(
        onDismiss = onDismiss,
        title = stringResource(Res.string.share_pick_trip_title),
    ) {
        Text(
            text = if (fileCount == 1) {
                stringResource(Res.string.share_pick_trip_one)
            } else {
                stringResource(Res.string.share_pick_trip_many, fileCount)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.thykra.textMeta,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SheetAction(
            label = stringResource(Res.string.share_new_trip),
            icon = ThykraIcons.Plus,
            onClick = onCreateTrip,
        )
        SheetDivider()

        when {
            loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    Box(Modifier.fillMaxWidth().height(44.dp).skeleton())
                }
            }

            trips.isEmpty() -> Text(
                text = stringResource(Res.string.share_no_trips),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.thykra.textMeta,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            else -> LazyColumn(
                // Capped so a person with forty trips still sees the sheet's own
                // affordances rather than a list that fills the screen.
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                // Newest activity first, the same order the Trips tab uses, so the trip
                // someone is most likely to mean sits under their thumb. Trips that have
                // never been touched sort last rather than first.
                val ordered = trips.sortedByDescending { it.lastActivityAt ?: it.createdAt }
                items(ordered, key = { it.id }) { trip ->
                    SheetAction(
                        label = trip.title,
                        icon = ThykraIcons.Trips,
                        supporting = tripSupporting(trip),
                        onClick = { onPick(trip) },
                    )
                }
            }
        }

        SheetDivider()
        SheetAction(label = stringResource(Res.string.share_cancel), onClick = onDismiss)
    }
}

/** "12 photos · 3 people", or nothing when the trip is empty and new. */
@Composable
private fun tripSupporting(trip: AlbumDto): String? {
    val parts = buildList {
        if (trip.mediaCount > 0) {
            add(stringResource(Res.string.common_photos_count, trip.mediaCount))
        }
        // > 1, not > 0: "1 people" is wrong, and a solo trip has nothing to say here.
        if (trip.memberCount > 1) {
            add(stringResource(Res.string.common_people_count, trip.memberCount))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/**
 * Holds the sheet's data. Separated from the composable so the shell can own the fetch
 * and the sheet stays a pure rendering of it.
 */
@Composable
fun rememberShareTrips(
    load: suspend () -> List<AlbumDto>,
): ShareTripsState {
    var state by remember { mutableStateOf(ShareTripsState(loading = true)) }
    LaunchedEffect(Unit) {
        state = runCatching { load() }
            .fold(
                onSuccess = { ShareTripsState(trips = it, loading = false) },
                // A failed list is not a reason to lose the photos: the sheet still
                // offers "New trip", which is the path that always works.
                onFailure = { ShareTripsState(loading = false) },
            )
    }
    return state
}

data class ShareTripsState(
    val trips: List<AlbumDto> = emptyList(),
    val loading: Boolean = false,
)
