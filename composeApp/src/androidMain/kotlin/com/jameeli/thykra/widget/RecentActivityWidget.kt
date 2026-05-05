package com.jameeli.thykra.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jameeli.thykra.MainActivity
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.theme.ThykraColors
import kotlinx.datetime.Instant

/**
 * Glance widget that shows recent reactions and comments for the user's configured album.
 *
 * NOTE: There is no aggregated `/api/activity/recent` endpoint yet, so this widget walks the
 * 5 most recent media items in the bound album and fetches reactions + comments for each.
 * That's `O(media * 2)` HTTP calls per refresh — fine at the default 1-hour interval but
 * not something we'd want to do on every navigation event.
 *
 * TODO: server-side aggregated activity endpoint — Phase 7 polish.
 */
class RecentActivityWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context, id)
        provideContent {
            RecentActivityContent(data)
        }
    }

    private suspend fun loadData(context: Context, id: GlanceId): RecentActivityData {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val albumId = prefs.albumId() ?: return RecentActivityData.NotConfigured
        val albumTitle = prefs.albumTitle() ?: ""

        val api = WidgetApi(context)
        return try {
            if (!api.isSignedIn()) return RecentActivityData.SignedOut

            val mediaList = api.media.getAlbumMedia(albumId).data.orEmpty()
                .filter { it.status == MediaStatus.ACTIVE }
                .sortedByDescending { it.uploadedAt }
                .take(MAX_MEDIA_TO_INSPECT)

            if (mediaList.isEmpty()) {
                return RecentActivityData.Loaded(albumId, albumTitle, emptyList())
            }

            val items = mutableListOf<ActivityItem>()
            for (media in mediaList) {
                runCatching {
                    val reactions = api.reactions.list(albumId, media.id).data?.reactions.orEmpty()
                    reactions.forEach { reaction ->
                        items += ActivityItem.Reaction(
                            albumId = albumId,
                            media = media,
                            actorName = reaction.userDisplayName,
                            type = reaction.type,
                            createdAt = reaction.createdAt
                        )
                    }
                }
                runCatching {
                    val comments = api.comments.list(albumId, media.id).data.orEmpty()
                    comments.forEach { comment ->
                        items += ActivityItem.Comment(
                            albumId = albumId,
                            media = media,
                            actorName = comment.authorDisplayName,
                            body = comment.body,
                            createdAt = comment.createdAt
                        )
                    }
                }
            }

            val recent = items.sortedByDescending { it.createdAt }.take(MAX_ITEMS)
            RecentActivityData.Loaded(albumId, albumTitle, recent)
        } catch (_: Throwable) {
            RecentActivityData.Error(albumId, albumTitle)
        } finally {
            api.close()
        }
    }

    companion object {
        private const val MAX_MEDIA_TO_INSPECT = 5
        private const val MAX_ITEMS = 10
    }
}

internal sealed interface ActivityItem {
    val albumId: String
    val media: MediaDto
    val actorName: String
    val createdAt: Instant

    data class Reaction(
        override val albumId: String,
        override val media: MediaDto,
        override val actorName: String,
        val type: ReactionType,
        override val createdAt: Instant
    ) : ActivityItem

    data class Comment(
        override val albumId: String,
        override val media: MediaDto,
        override val actorName: String,
        val body: String,
        override val createdAt: Instant
    ) : ActivityItem
}

internal sealed interface RecentActivityData {
    data object NotConfigured : RecentActivityData
    data object SignedOut : RecentActivityData
    data class Error(val albumId: String, val albumTitle: String) : RecentActivityData
    data class Loaded(
        val albumId: String,
        val albumTitle: String,
        val items: List<ActivityItem>
    ) : RecentActivityData
}

@Composable
private fun RecentActivityContent(data: RecentActivityData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(ThykraColors.WarmWhite))
            .cornerRadius(16.dp)
    ) {
        when (data) {
            RecentActivityData.NotConfigured -> CenterMessage("Tap to pick an album", null)
            RecentActivityData.SignedOut -> CenterMessage("Sign in to Thykra", null)
            is RecentActivityData.Error -> CenterMessage("Couldn't load activity", data.albumId)
            is RecentActivityData.Loaded -> if (data.items.isEmpty()) {
                CenterMessage("No recent activity in ${data.albumTitle}", data.albumId)
            } else {
                ActivityList(data)
            }
        }
    }
}

@Composable
private fun ActivityList(data: RecentActivityData.Loaded) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "Recent in ${data.albumTitle}".trim(),
            style = TextStyle(
                color = ColorProvider(ThykraColors.DeepNavy),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(6.dp))
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(data.items) { item ->
                ActivityRow(item)
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val context = LocalContext.current
    val tapAction = actionStartActivity(
        WidgetDeepLinks.openAlbum(context, item.albumId, item.media.id)
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(tapAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = badgeFor(item),
            style = TextStyle(fontSize = 16.sp)
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = lineFor(item),
                style = TextStyle(
                    color = ColorProvider(ThykraColors.DeepNavy),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2
            )
            Text(
                text = formatRelative(item.createdAt),
                style = TextStyle(
                    color = ColorProvider(ThykraColors.MutedSlate),
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CenterMessage(text: String, albumId: String?) {
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
            .padding(16.dp)
            .clickable(tapAction),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(ThykraColors.DeepNavy),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun badgeFor(item: ActivityItem): String = when (item) {
    is ActivityItem.Comment -> "💬" // 💬
    is ActivityItem.Reaction -> when (item.type) {
        ReactionType.LOVE -> "❤️" // ❤️
        ReactionType.WOW -> "😲" // 😲
        ReactionType.LAUGH -> "😂" // 😂
        ReactionType.WISH_I_WAS_THERE -> "✨" // ✨
        ReactionType.WANDERLUST -> "🌍" // 🌍
        ReactionType.BEACH -> "🏖️" // 🏖️
        ReactionType.MOUNTAIN -> "⛰️" // ⛰️
        ReactionType.FOOD -> "🍴" // 🍴
    }
}

private fun lineFor(item: ActivityItem): String = when (item) {
    is ActivityItem.Reaction -> "${item.actorName} reacted to ${item.media.filename}"
    is ActivityItem.Comment -> "${item.actorName}: ${item.body.take(80)}"
}
