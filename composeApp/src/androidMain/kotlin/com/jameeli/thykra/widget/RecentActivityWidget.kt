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
import com.jameeli.thykra.model.ActivityItemDto
import com.jameeli.thykra.model.ActivityType
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.ui.theme.ThykraColors

/**
 * Glance widget that shows recent reactions and comments across the user's albums.
 *
 * Backed by the aggregated `GET /api/activity/recent` endpoint — one HTTP call per refresh,
 * regardless of how many albums or media items the user owns.
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
        // The widget config activity is shared with LatestPhotoWidget and stores a
        // selected album. We don't need it for the activity feed (the endpoint already
        // scopes to the caller's albums), but we keep the "NotConfigured" state so an
        // un-configured widget instance prompts the user — same UX as before.
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        if (prefs.albumId() == null) return RecentActivityData.NotConfigured

        val api = WidgetApi(context)
        return try {
            if (!api.isSignedIn()) return RecentActivityData.SignedOut

            val response = api.activity.recent(limit = MAX_ITEMS)
            val items = response.data?.items.orEmpty()
            RecentActivityData.Loaded(items)
        } catch (_: Throwable) {
            RecentActivityData.Error
        } finally {
            api.close()
        }
    }

    companion object {
        private const val MAX_ITEMS = 10
    }
}

internal sealed interface RecentActivityData {
    data object NotConfigured : RecentActivityData
    data object SignedOut : RecentActivityData
    data object Error : RecentActivityData
    data class Loaded(val items: List<ActivityItemDto>) : RecentActivityData
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
            RecentActivityData.NotConfigured -> CenterMessage("Tap to set up", openAlbum = null)
            RecentActivityData.SignedOut -> CenterMessage("Sign in to Thykra", openAlbum = null)
            RecentActivityData.Error -> CenterMessage("Couldn't load activity", openAlbum = null)
            is RecentActivityData.Loaded -> if (data.items.isEmpty()) {
                CenterMessage("No recent activity", openAlbum = null)
            } else {
                ActivityList(data.items)
            }
        }
    }
}

@Composable
private fun ActivityList(items: List<ActivityItemDto>) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "Recent activity",
            style = TextStyle(
                color = ColorProvider(ThykraColors.DeepNavy),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(6.dp))
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(items) { item ->
                ActivityRow(item)
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItemDto) {
    val context = LocalContext.current
    val tapAction = actionStartActivity(
        WidgetDeepLinks.openAlbum(context, item.albumId, item.mediaId)
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
                text = "in ${item.albumTitle}",
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
private fun CenterMessage(text: String, openAlbum: String?) {
    val context = LocalContext.current
    val tapAction = actionStartActivity(
        if (openAlbum != null) WidgetDeepLinks.openAlbum(context, openAlbum)
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

private fun badgeFor(item: ActivityItemDto): String = when (item.type) {
    ActivityType.COMMENT -> "💬"
    ActivityType.REACTION -> when (item.reactionType) {
        ReactionType.LOVE -> "❤️"
        ReactionType.WOW -> "😲"
        ReactionType.LAUGH -> "😂"
        ReactionType.WISH_I_WAS_THERE -> "✨"
        ReactionType.WANDERLUST -> "🌍"
        ReactionType.BEACH -> "🏖️"
        ReactionType.MOUNTAIN -> "⛰️"
        ReactionType.FOOD -> "🍴"
        null -> "•"
    }
}

private fun lineFor(item: ActivityItemDto): String = when (item.type) {
    ActivityType.REACTION -> "${item.actorDisplayName} reacted"
    ActivityType.COMMENT -> "${item.actorDisplayName}: ${item.commentBody.orEmpty().take(80)}"
}
