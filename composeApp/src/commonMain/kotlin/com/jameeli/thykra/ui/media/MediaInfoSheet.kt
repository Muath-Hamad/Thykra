package com.jameeli.thykra.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.resources.Res
import com.jameeli.thykra.resources.info_added
import com.jameeli.thykra.resources.info_added_by
import com.jameeli.thykra.resources.info_length
import com.jameeli.thykra.resources.info_no_date
import com.jameeli.thykra.resources.info_size
import com.jameeli.thykra.resources.info_taken
import com.jameeli.thykra.resources.info_type
import com.jameeli.thykra.resources.viewer_photo_info
import org.jetbrains.compose.resources.stringResource
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.ThykraSheet
import com.jameeli.thykra.ui.kit.formatChapterDate
import com.jameeli.thykra.ui.theme.thykra
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Design part 3 §07. What the Info pill opens.
 *
 * Facts only, in the order someone asks for them: who took it, when it was taken, what
 * size it is, and when it arrived. A photograph with no date says "No date" rather than
 * quietly borrowing its upload time — the two mean different things.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(
    media: MediaDto,
    uploaderName: String?,
    onDismiss: () -> Unit,
) {
    ThykraSheet(onDismiss = onDismiss, title = stringResource(Res.string.viewer_photo_info)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            uploaderName?.let { InfoRow(stringResource(Res.string.info_added_by), it) }
            InfoRow(
                label = stringResource(Res.string.info_taken),
                value = media.takenAt?.let { formatTimestamp(it) }
                    ?: stringResource(Res.string.info_no_date),
            )
            if (media.width != null && media.height != null) {
                InfoRow(stringResource(Res.string.info_size), "${media.width} × ${media.height}")
            }
            InfoRow(stringResource(Res.string.info_type), media.contentType)
            InfoRow(stringResource(Res.string.info_added), formatTimestamp(media.uploadedAt))
            media.durationMs?.let { InfoRow(stringResource(Res.string.info_length), formatLength(it)) }
        }
        SheetDivider()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.thykra.textMeta,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatTimestamp(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when (val h = local.hour % 12) {
        0 -> 12
        else -> h
    }
    val suffix = if (local.hour < 12) "am" else "pm"
    val minute = local.minute.toString().padStart(2, '0')
    return "${formatChapterDate(local.date)}, $hour12:$minute $suffix"
}

private fun formatLength(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
