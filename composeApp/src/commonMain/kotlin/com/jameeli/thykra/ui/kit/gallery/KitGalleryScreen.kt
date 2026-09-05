package com.jameeli.thykra.ui.kit.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.jameeli.thykra.model.AlbumDto
import com.jameeli.thykra.model.AlbumMemberSummary
import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MediaDto
import com.jameeli.thykra.model.MediaStatus
import com.jameeli.thykra.model.MediaType
import com.jameeli.thykra.model.MemberRole
import com.jameeli.thykra.model.ReactionSummaryDto
import com.jameeli.thykra.model.ReactionType
import com.jameeli.thykra.chapters.groupIntoChapters
import com.jameeli.thykra.ui.kit.ActivityCard
import com.jameeli.thykra.ui.kit.AssistChip
import com.jameeli.thykra.ui.kit.Avatar
import com.jameeli.thykra.ui.kit.AvatarSize
import com.jameeli.thykra.ui.kit.AvatarStack
import com.jameeli.thykra.ui.kit.AvatarUser
import com.jameeli.thykra.ui.kit.BadgeTone
import com.jameeli.thykra.ui.kit.ChapterHeader
import com.jameeli.thykra.ui.kit.ChapterSkeleton
import com.jameeli.thykra.ui.kit.EmptyGlyph
import com.jameeli.thykra.ui.kit.EmptyState
import com.jameeli.thykra.ui.kit.FilterChip
import com.jameeli.thykra.ui.kit.NavBarState
import com.jameeli.thykra.ui.kit.OfflineBanner
import com.jameeli.thykra.ui.kit.PinnedChapterBar
import com.jameeli.thykra.ui.kit.Plate
import com.jameeli.thykra.ui.kit.ReactionBar
import com.jameeli.thykra.ui.kit.RecapCard
import com.jameeli.thykra.ui.kit.RootTab
import com.jameeli.thykra.ui.kit.Segmented
import com.jameeli.thykra.ui.kit.SegmentedOption
import com.jameeli.thykra.ui.kit.SheetAction
import com.jameeli.thykra.ui.kit.SheetDivider
import com.jameeli.thykra.ui.kit.Stamp
import com.jameeli.thykra.ui.kit.StampTone
import com.jameeli.thykra.ui.kit.ThykraBadge
import com.jameeli.thykra.ui.kit.ThykraButton
import com.jameeli.thykra.ui.kit.ThykraButtonSpec
import com.jameeli.thykra.ui.kit.ThykraButtonVariant
import com.jameeli.thykra.ui.kit.ThykraNavigationBar
import com.jameeli.thykra.ui.kit.ThykraTextField
import com.jameeli.thykra.ui.kit.TripActionBar
import com.jameeli.thykra.ui.kit.TripCard
import com.jameeli.thykra.ui.kit.TripCardSkeleton
import com.jameeli.thykra.ui.kit.UploadBatch
import com.jameeli.thykra.ui.kit.UploadDock
import com.jameeli.thykra.ui.kit.UploadRowState
import com.jameeli.thykra.ui.kit.UploadRowStatus
import com.jameeli.thykra.ui.kit.ViewerChrome
import com.jameeli.thykra.ui.kit.clayPhrase
import com.jameeli.thykra.ui.theme.ThemeMode
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.ThykraTheme
import com.jameeli.thykra.ui.theme.thykra
import kotlinx.datetime.Instant

/**
 * The kit gallery — build step 02's own acceptance test, and the checklist the design's
 * boards are compared against.
 *
 * Every part is drawn in Paper and Darkroom side by side, in every state it has. The
 * toolbar switches font scale to 2x and layout direction to RTL, which are the two
 * conditions that break layouts and the two the design asks to be checked before
 * anything ships.
 *
 * It is reachable only from a debug build; see `KitGalleryRoute`.
 */
@Composable
fun KitGalleryScreen(onClose: () -> Unit = {}) {
    var fontScale by remember { mutableStateOf(1f) }
    var rtl by remember { mutableStateOf(false) }
    val baseDensity = LocalDensity.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        GalleryToolbar(
            fontScale = fontScale,
            onFontScale = { fontScale = it },
            rtl = rtl,
            onRtl = { rtl = it },
            onClose = onClose,
        )

        CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density, fontScale),
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp),
            ) {
                KitSpecimen.entries.forEach { section ->
                    item(key = section.name) {
                        SectionRow(section)
                    }
                }
            }
        }
    }
}

/**
 * Every board in design part 2, as an enumerable specimen — so the gallery screen and the
 * snapshot test draw exactly the same thing rather than drifting apart.
 */
enum class KitSpecimen(val title: String) {
    Button("4.1 Button"),
    Avatars("4.2 Avatar, AvatarStack, Stamp"),
    Cards("4.3 Cards"),
    Sheet("4.4 Sheet rows"),
    Toast("4.5 Dialog and Toast"),
    Inputs("4.6 Input, Segmented, Chip"),
    Empty("4.7 Skeleton and EmptyState"),
    Dock("4.8 UploadDock and UploadRow"),
    Reactions("4.9 ReactionBar"),
    Chapters("4.10 ChapterHeader"),
    Chrome("4.11 Plate, bars, viewer chrome"),
    Icons("03 The icon set"),
}

/** One section, drawn twice: Paper on the start side, Darkroom beside it. */
@Composable
private fun SectionRow(section: KitSpecimen) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.thykra.textMeta,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(ThemeMode.Paper, ThemeMode.Darkroom).forEach { mode ->
                ThykraTheme(mode = mode) {
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        section.Content()
                    }
                }
            }
        }
    }
}

/** The specimen itself, without the gallery's chrome around it. */
@Composable
fun KitSpecimen.Content() {
    when (this) {
        KitSpecimen.Button -> ButtonSpecimens()
        KitSpecimen.Avatars -> AvatarSpecimens()
        KitSpecimen.Cards -> CardSpecimens()
        KitSpecimen.Sheet -> SheetSpecimens()
        KitSpecimen.Toast -> DialogSpecimens()
        KitSpecimen.Inputs -> InputSpecimens()
        KitSpecimen.Empty -> EmptySpecimens()
        KitSpecimen.Dock -> DockSpecimens()
        KitSpecimen.Reactions -> ReactionSpecimens()
        KitSpecimen.Chapters -> ChapterSpecimens()
        KitSpecimen.Chrome -> ChromeSpecimens()
        KitSpecimen.Icons -> IconSpecimens()
    }
}

// ── Specimens ─────────────────────────────────────────────────────────────────

@Composable
private fun ButtonSpecimens() {
    ThykraButtonVariant.entries.forEach { variant ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThykraButton(variant.name, {}, variant = variant, icon = ThykraIcons.Plus)
            ThykraButton("Off", {}, variant = variant, enabled = false)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThykraButton("Creating…", {}, loading = true)
        ThykraButton("Delete trip", {}, variant = ThykraButtonVariant.Text, destructive = true)
    }
}

@Composable
private fun AvatarSpecimens() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarSize.entries.forEach { size ->
            Avatar(user = SampleUsers[0], size = size)
        }
    }
    AvatarStack(users = SampleUsers, max = 3, totalCount = 6)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stamp(eyebrow = "Invited by", name = "Sara Nasser")
        Stamp(eyebrow = "Link", name = "Expired", tone = StampTone.Warning)
    }
}

@Composable
private fun CardSpecimens() {
    TripCard(
        album = SampleAlbum,
        meta = "84 photos · 6 people · Apr 2026",
        lastActivity = "Sara added 12 photos · yesterday",
        videoCount = 3,
        onClick = {},
    )
    ActivityCard(
        actor = SampleUsers[0],
        sentence = "added 12 photos",
        meta = "Wadi Rum · yesterday",
        unseen = true,
    )
    ActivityCard(
        actor = SampleUsers[1],
        sentence = "and 2 others reacted to 4 photos",
        meta = "Wadi Rum · 2 h",
    )
    RecapCard(
        title = "Four days in Wadi Rum",
        eyebrow = "Recap · 18 photos",
        coverUrl = null,
        onClick = {},
    )
}

@Composable
private fun SheetSpecimens() {
    SheetAction("View their photos", {}, icon = ThykraIcons.Grid)
    SheetDivider()
    SheetAction("Invite friends", {}, icon = ThykraIcons.PersonAdd, supporting = "Creates a 7-day link you can share")
    SheetDivider()
    SheetAction("Remove from trip", {}, destructive = true)
}

@Composable
private fun DialogSpecimens() {
    // The dialog itself opens in a window, so the gallery shows the parts it is made of.
    Text("Delete Wadi Rum?", style = MaterialTheme.typography.headlineMedium)
    Text(
        "84 photos and 3 videos from 6 people will be gone for everyone. This can't be undone.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThykraButton("Keep it", {}, variant = ThykraButtonVariant.Text)
        ThykraButton("Delete trip", {}, destructive = true)
    }
}

@Composable
private fun InputSpecimens() {
    var text by remember { mutableStateOf("Wadi Rum, April") }
    ThykraTextField(
        value = "",
        onValueChange = {},
        label = "Trip title",
        placeholder = "Where did you go?",
    )
    ThykraTextField(
        value = text,
        onValueChange = { text = it },
        label = "Trip title",
        helper = "Friends will see this",
        maxLength = 60,
    )
    ThykraTextField(
        value = "",
        onValueChange = {},
        label = "Trip title",
        error = "Give the trip a name",
    )
    var segment by remember { mutableStateOf(0) }
    Segmented(
        options = listOf(
            SegmentedOption("Days", ThykraIcons.Chapters),
            SegmentedOption("Sheet", ThykraIcons.Grid),
        ),
        selectedIndex = segment,
        onSelect = { segment = it },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip("Expires in 7 days", {})
        FilterChip("Photos only", selected = true, onToggle = {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThykraBadge("Owner", tone = BadgeTone.People)
        ThykraBadge("Viewer", tone = BadgeTone.Muted)
        ThykraBadge("Expired", tone = BadgeTone.Warning)
    }
}

@Composable
private fun EmptySpecimens() {
    TripCardSkeleton()
    OfflineBanner(visible = true)
    EmptyState(
        headline = clayPhrase("Nothing here ", "yet."),
        body = "Add the first photos and the trip starts telling itself by day.",
        glyph = EmptyGlyph.Plate,
        primary = ThykraButtonSpec("Add photos", {}, icon = ThykraIcons.Plus),
    )
    EmptyState(
        headline = buildAnnotatedString { append("You're offline.") },
        body = "Your trips are still here. New photos will upload when you're back.",
        glyph = EmptyGlyph.Offline,
        primary = ThykraButtonSpec("Try again", {}, ThykraButtonVariant.Outlined, ThykraIcons.Retry),
    )
}

@Composable
private fun DockSpecimens() {
    UploadDock(batch = SampleBatch, expanded = true, onToggle = {})
    UploadDock(batch = SampleBatch.copy(connected = false), expanded = false, onToggle = {})
    UploadDock(
        batch = SampleBatch.copy(
            rows = SampleBatch.rows.map { it.copy(status = UploadRowStatus.Done) },
            celebrationDetail = "Sorted into 3 days",
        ),
        expanded = false,
        onToggle = {},
    )
}

@Composable
private fun ReactionSpecimens() {
    ReactionBar(reactions = SampleReactions, onToggle = {}, onOpenPicker = {})
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        ReactionBar(
            reactions = SampleReactions,
            onToggle = {},
            onOpenPicker = {},
            overMedia = true,
        )
    }
}

@Composable
private fun ChapterSpecimens() {
    val chapters = groupIntoChapters(SampleMedia)
    chapters.firstOrNull()?.let { chapter ->
        ChapterHeader(chapter = chapter, contributors = SampleUsers.take(2))
        PinnedChapterBar(chapter = chapter)
    }
    ChapterSkeleton()
}

@Composable
private fun ChromeSpecimens() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Plate(media = SampleMedia[0], modifier = Modifier.weight(1f), aspectRatio = 1f)
        Plate(
            media = SampleMedia[1],
            modifier = Modifier.weight(1f),
            aspectRatio = 1f,
            selected = true,
        )
    }
    ThykraNavigationBar(state = NavBarState(RootTab.Trips, activityDot = true), onSelect = {})
    TripActionBar(role = MemberRole.OWNER, onAddPhotos = {}, onShare = {}, onMore = {})
    TripActionBar(role = MemberRole.VIEWER, onAddPhotos = {}, onShare = {}, onMore = {})
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ViewerChrome(
            positionLabel = "14 / 31 · Sat 12 Apr",
            reactions = SampleReactions,
            commentCount = 3,
            visible = true,
            onBack = {},
            onInfo = {},
            onMore = {},
            onToggleReaction = {},
            onOpenPicker = {},
            onComments = {},
            attribution = "Sara Nasser · 4:12 pm",
            filmstrip = SampleMedia,
        )
    }
}

@Composable
private fun IconSpecimens() {
    val icons = listOf(
        "Back" to ThykraIcons.Back, "Chevron" to ThykraIcons.Chevron,
        "Close" to ThykraIcons.Close, "More" to ThykraIcons.More,
        "Plus" to ThykraIcons.Plus, "Check" to ThykraIcons.Check,
        "Share" to ThykraIcons.Share, "Link" to ThykraIcons.Link,
        "Copy" to ThykraIcons.Copy, "Settings" to ThykraIcons.Settings,
        "People" to ThykraIcons.People, "PersonAdd" to ThykraIcons.PersonAdd,
        "Person" to ThykraIcons.Person, "Comment" to ThykraIcons.Comment,
        "React" to ThykraIcons.React, "Info" to ThykraIcons.Info,
        "Alert" to ThykraIcons.Alert, "Retry" to ThykraIcons.Retry,
        "Play" to ThykraIcons.Play, "Pause" to ThykraIcons.Pause,
        "Volume" to ThykraIcons.Volume, "Mute" to ThykraIcons.Mute,
        "Grid" to ThykraIcons.Grid, "Chapters" to ThykraIcons.Chapters,
        "Trips" to ThykraIcons.Trips, "TripsFilled" to ThykraIcons.TripsFilled,
        "Activity" to ThykraIcons.Activity, "ActivityFilled" to ThykraIcons.ActivityFilled,
        "Recaps" to ThykraIcons.Recaps, "RecapsFilled" to ThykraIcons.RecapsFilled,
        "Video" to ThykraIcons.Video, "Offline" to ThykraIcons.Offline,
        "Lock" to ThykraIcons.Lock, "Globe" to ThykraIcons.Globe,
    )
    icons.chunked(6).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (name, vector) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp),
                ) {
                    Icon(vector, contentDescription = name, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
    Text(
        "${icons.size} glyphs",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.thykra.textMeta,
    )
}

@Composable
private fun GalleryToolbar(
    fontScale: Float,
    onFontScale: (Float) -> Unit,
    rtl: Boolean,
    onRtl: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Kit gallery",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            label = "2x type",
            selected = fontScale > 1f,
            onToggle = { onFontScale(if (fontScale > 1f) 1f else 2f) },
        )
        FilterChip(label = "RTL", selected = rtl, onToggle = { onRtl(!rtl) })
        ThykraButton("Close", onClose, variant = ThykraButtonVariant.Text)
    }
}

// ── Sample data ───────────────────────────────────────────────────────────────

private val SampleUsers = listOf(
    AvatarUser("u1", "Sara Nasser"),
    AvatarUser("u2", "Omar Khalil"),
    AvatarUser("u3", "Lina Haddad"),
)

private val SampleAlbum = AlbumDto(
    id = "a1",
    ownerId = "u1",
    title = "Wadi Rum, April",
    description = "Four days in the desert with the usual suspects",
    coverUrl = null,
    visibility = AlbumVisibility.LINK_SHARED,
    memberCount = 6,
    previewMembers = SampleUsers.map { AlbumMemberSummary(it.id, it.displayName) },
    createdAt = Instant.parse("2026-04-02T10:00:00Z"),
)

private fun sampleMedia(id: String, hour: Int, type: MediaType = MediaType.PHOTO) = MediaDto(
    id = id,
    albumId = "a1",
    uploaderId = "u1",
    type = type,
    status = MediaStatus.ACTIVE,
    storageKey = "a1/$id.jpg",
    url = "",
    thumbnailUrl = null,
    filename = "IMG_40$id.jpg",
    contentType = "image/jpeg",
    fileSize = 2_400_000,
    width = 4032,
    height = 3024,
    durationMs = if (type == MediaType.VIDEO) 42_000 else null,
    takenAt = Instant.parse("2026-04-12T0$hour:00:00Z"),
    uploadedAt = Instant.parse("2026-04-13T10:00:00Z"),
)

private val SampleMedia = listOf(
    sampleMedia("1", 6),
    sampleMedia("2", 7, MediaType.VIDEO),
    sampleMedia("3", 8),
    sampleMedia("4", 9),
)

private val SampleReactions = listOf(
    ReactionSummaryDto(ReactionType.WANDERLUST, 4, reactedByMe = true),
    ReactionSummaryDto(ReactionType.LOVE, 2, reactedByMe = false),
    ReactionSummaryDto(ReactionType.MOUNTAIN, 1, reactedByMe = false),
)

private val SampleBatch = UploadBatch(
    id = "batch1",
    tripId = "a1",
    tripTitle = "Wadi Rum, April",
    secondsRemaining = 240,
    rows = listOf(
        UploadRowState("1", "IMG_4018.jpg", UploadRowStatus.Failed, bytesUploaded = 0, totalBytes = 2_400_000),
        UploadRowState(
            "2",
            "IMG_4012.jpg",
            UploadRowStatus.Failed,
            failureReason = "Too large (2.1 GB)",
            retryable = false,
            totalBytes = 2_100_000_000,
        ),
        UploadRowState("3", "IMG_4021.jpg", UploadRowStatus.Uploading, bytesUploaded = 1_500_000, totalBytes = 2_400_000),
        UploadRowState("4", "VID_0930.mp4", UploadRowStatus.Confirming, isVideo = true, bytesUploaded = 8_000_000, totalBytes = 8_000_000),
        UploadRowState("5", "IMG_4019.jpg", UploadRowStatus.Queued, totalBytes = 2_400_000),
        UploadRowState("6", "IMG_4017.jpg", UploadRowStatus.Done, bytesUploaded = 2_400_000, totalBytes = 2_400_000),
    ),
)
