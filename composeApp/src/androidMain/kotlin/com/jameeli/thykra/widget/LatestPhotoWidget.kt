package com.jameeli.thykra.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jameeli.thykra.resolveAgainstApiOrigin
import com.jameeli.thykra.MainActivity
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MediaType
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Glance widget showing the latest photo from the user's configured album.
 *
 * Update flow:
 *  1. Read [WidgetPrefs.SELECTED_ALBUM_ID] from per-instance Glance state.
 *  2. If unconfigured / signed out, show a friendly call to action.
 *  3. Otherwise call `GET /api/albums/{id}/media`, take the most recent ACTIVE photo,
 *     download its thumbnail bytes, and render full-bleed with a dark gradient bottom strip
 *     containing the album title + relative date.
 *  4. The whole widget is clickable and deep-links to [MainActivity] with the album/media id
 *     extras (consumed by the navigation host).
 */
class LatestPhotoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context, id)
        provideContent {
            LatestPhotoContent(data)
        }
    }

    private suspend fun loadData(context: Context, id: GlanceId): LatestPhotoData {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val albumId = prefs.albumId()
        val albumTitle = prefs.albumTitle()
        if (albumId.isNullOrBlank()) return LatestPhotoData.NotConfigured

        val api = WidgetApi(context)
        return try {
            if (!api.isSignedIn()) return LatestPhotoData.SignedOut
            val mediaResponse = api.media.getAlbumMedia(albumId)
            val list = mediaResponse.data.orEmpty()
            val latestPhoto = list
                .filter { it.status == MediaStatus.ACTIVE && it.type == MediaType.PHOTO }
                .maxByOrNull { it.uploadedAt }
                ?: return LatestPhotoData.Empty(albumId, albumTitle ?: "")

            val bitmap = downloadThumbnail(api, latestPhoto)
            LatestPhotoData.Photo(
                albumId = albumId,
                albumTitle = albumTitle ?: "",
                mediaId = latestPhoto.id,
                bitmap = bitmap,
                uploadedAt = latestPhoto.uploadedAt
            )
        } catch (_: Throwable) {
            LatestPhotoData.Error(albumId, albumTitle ?: "")
        } finally {
            api.close()
        }
    }

    private suspend fun downloadThumbnail(api: WidgetApi, media: MediaDto): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val url = resolveAgainstApiOrigin(media.thumbnailUrl ?: media.url)
                val bytes = api.client.get(url).bodyAsBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Throwable) {
                null
            }
        }
}

/**
 * Snapshot of the data needed to render [LatestPhotoWidget]. Sealed so the composable can
 * exhaust each rendering branch.
 */
internal sealed interface LatestPhotoData {
    data object NotConfigured : LatestPhotoData
    data object SignedOut : LatestPhotoData
    data class Empty(val albumId: String, val albumTitle: String) : LatestPhotoData
    data class Error(val albumId: String, val albumTitle: String) : LatestPhotoData
    data class Photo(
        val albumId: String,
        val albumTitle: String,
        val mediaId: String,
        val bitmap: Bitmap?,
        val uploadedAt: Instant
    ) : LatestPhotoData
}

@Composable
private fun LatestPhotoContent(data: LatestPhotoData) {
    ThykraGlanceTheme { LatestPhotoBody(data) }
}

@Composable
private fun LatestPhotoBody(data: LatestPhotoData) {
    when (data) {
        LatestPhotoData.NotConfigured -> WidgetMessage("Tap to pick an album", albumId = null)
        LatestPhotoData.SignedOut -> WidgetMessage("Sign in to Thykra", albumId = null)
        is LatestPhotoData.Empty -> WidgetMessage("No photos yet in ${data.albumTitle}", data.albumId)
        is LatestPhotoData.Error -> WidgetMessage("Couldn't load — tap to open", data.albumId)
        is LatestPhotoData.Photo -> PhotoCard(data)
    }
}

@Composable
private fun PhotoCard(data: LatestPhotoData.Photo) {
    val context = LocalContext.current
    val tapAction = actionStartActivity(
        WidgetDeepLinks.openAlbum(context, data.albumId, data.mediaId)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(20.dp)
            .clickable(tapAction),
        contentAlignment = Alignment.BottomStart
    ) {
        if (data.bitmap != null) {
            Image(
                provider = ImageProvider(data.bitmap),
                contentDescription = data.albumTitle,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Bottom strip — Glance can't render gradients, so we use a semi-opaque solid panel.
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                // Glance cannot render a gradient, so the caption sits on a solid
                // near-opaque panel in the same ink the app uses for its scrims.
                .background(ColorProvider(ComposeColor(0xE6121114)))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = data.albumTitle.ifBlank { "Album" },
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(0xFFF2EBDF)),
                        fontSize = WidgetType.TITLE_SP.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = formatRelative(data.uploadedAt),
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(0xFFABA3A0)),
                        fontSize = WidgetType.META_SP.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WidgetMessage(text: String, albumId: String?) {
    val context = LocalContext.current
    val tapAction = actionStartActivity(
        if (albumId != null) WidgetDeepLinks.openAlbum(context, albumId)
        else Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .clickable(tapAction)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = WidgetType.TITLE_SP.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/** Format an Instant as a short relative string ("2h ago", "Yesterday", "3d ago"). */
internal fun formatRelative(instant: Instant): String {
    val now = Clock.System.now()
    val deltaMs = (now.toEpochMilliseconds() - instant.toEpochMilliseconds()).coerceAtLeast(0)
    val minutes = deltaMs / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
