package com.jameeli.thykra.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.ui.theme.ThykraTheme
import kotlinx.coroutines.launch

/**
 * Configuration activity launched by the system when the user adds a Thykra widget.
 *
 * Flow:
 *  1. Activity starts with `AppWidgetManager.EXTRA_APPWIDGET_ID` in extras.
 *  2. We default the result to `RESULT_CANCELED` so cancelling removes the widget.
 *  3. Compose UI lists the user's albums (via [AlbumApi]); user picks one + refresh interval.
 *  4. On Save: persist into per-widget Glance state via [saveWidgetConfig], trigger an
 *     immediate update on the right [GlanceAppWidget] subclass, then `RESULT_OK` and finish.
 *
 * The same activity is reused for both [LatestPhotoWidget] and [RecentActivityWidget]; we
 * detect which one to update by reading the receiver's component name from
 * [AppWidgetManager.getAppWidgetInfo].
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            ThykraTheme {
                ConfigScreen(
                    onCancel = { finish() },
                    onSave = { album, intervalMs -> saveAndFinish(album, intervalMs) }
                )
            }
        }
    }

    private fun saveAndFinish(album: AlbumDto, refreshIntervalMs: Long) {
        lifecycleScope.launch {
            try {
                val manager = GlanceAppWidgetManager(applicationContext)
                val glanceId = manager.getGlanceIdBy(appWidgetId)

                saveWidgetConfig(
                    context = applicationContext,
                    glanceId = glanceId,
                    albumId = album.id,
                    albumTitle = album.title,
                    refreshIntervalMs = refreshIntervalMs
                )

                // Find which widget class is bound to this appWidgetId so we can trigger a
                // targeted update — we don't want LatestPhotoWidget to re-render when the
                // user is configuring RecentActivityWidget and vice-versa.
                widgetForAppWidgetId(appWidgetId)?.update(applicationContext, glanceId)
            } catch (t: Throwable) {
                android.util.Log.e("WidgetConfig", "save failed", t)
            }
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            )
            finish()
        }
    }

    /**
     * Resolves which [GlanceAppWidget] subclass owns the given app-widget id by inspecting
     * the bound provider's [ComponentName].
     */
    private fun widgetForAppWidgetId(id: Int): GlanceAppWidget? {
        val info = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetInfo(id)
            ?: return null
        return when (info.provider.className) {
            LatestPhotoWidgetReceiver::class.java.name -> LatestPhotoWidget()
            RecentActivityWidgetReceiver::class.java.name -> RecentActivityWidget()
            else -> null
        }
    }
}

/**
 * UI for [WidgetConfigActivity]. Loads albums asynchronously and renders the picker.
 */
@Composable
private fun ConfigScreen(
    onCancel: () -> Unit,
    onSave: (AlbumDto, Long) -> Unit
) {
    val context = LocalContext.current
    var albums by remember { mutableStateOf<List<AlbumDto>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var signedIn by remember { mutableStateOf(true) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var selectedIntervalMs by remember { mutableStateOf(WidgetPrefs.DEFAULT_REFRESH_INTERVAL_MS) }

    LaunchedEffect(Unit) {
        val api = WidgetApi(context)
        try {
            if (!api.isSignedIn()) {
                signedIn = false
                albums = emptyList()
                return@LaunchedEffect
            }
            val response = api.albums.getAlbums()
            val list = response.data.orEmpty()
            albums = list
            selectedAlbumId = list.firstOrNull()?.id
        } catch (t: Throwable) {
            loadError = t.message ?: "Couldn't load albums"
            albums = emptyList()
        } finally {
            api.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Pick an album",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            when {
                albums == null -> LoadingBlock()
                !signedIn -> InfoBlock("Sign in to Thykra first, then add the widget.")
                loadError != null -> InfoBlock("Couldn't load albums: $loadError")
                albums.isNullOrEmpty() -> InfoBlock("No albums yet — create one in Thykra first.")
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(albums!!) { album ->
                            AlbumRow(
                                album = album,
                                selected = album.id == selectedAlbumId,
                                onSelect = { selectedAlbumId = album.id }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Refresh interval",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    IntervalRow(
                        labels = listOf(
                            "30 min" to 30 * 60 * 1000L,
                            "1 hour" to 60 * 60 * 1000L,
                            "6 hours" to 6 * 60 * 60 * 1000L
                        ),
                        selected = selectedIntervalMs,
                        onSelect = { selectedIntervalMs = it }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val picked = albums?.firstOrNull { it.id == selectedAlbumId }
                        if (picked != null) onSave(picked, selectedIntervalMs)
                    },
                    enabled = selectedAlbumId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumDto, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "${album.memberCount} member${if (album.memberCount == 1) "" else "s"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun IntervalRow(
    labels: List<Pair<String, Long>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { (label, ms) ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (ms == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = { onSelect(ms) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (ms == selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun InfoBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}
