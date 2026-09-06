package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.chapters.Chapter
import com.jameeli.thykra.chapters.ChapterMedia
import com.jameeli.thykra.chapters.UndatedNumeral
import com.jameeli.thykra.chapters.formatOrdinal
import com.jameeli.thykra.ui.theme.LocalArabic
import com.jameeli.thykra.ui.theme.numeralStyle
import com.jameeli.thykra.ui.theme.thykra
import kotlinx.datetime.LocalDate

/**
 * Part 2 §4.10. The numeral, the date, the counts, the people — then a 2 dp rule and the
 * day's photographs.
 *
 * The expanded header does not shrink in place: that fights LazyColumn recycling. The
 * collapse is [PinnedChapterBar] fading in over the last 40 dp of this header's exit, so
 * the collapse fraction lives on the bar rather than here.
 */
@Composable
fun <T : ChapterMedia> ChapterHeader(
    chapter: Chapter<T>,
    contributors: List<AvatarUser>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val arabic = LocalArabic.current

    val numeral = chapter.ordinal?.let { formatOrdinal(it, arabicIndic = arabic) } ?: UndatedNumeral
    val dateLabel = if (chapter.dated) {
        formatChapterDate(chapter.date)
    } else {
        "Added later"
    }
    val counts = chapterCounts(chapter)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .semantics(mergeDescendants = true) {
                heading()
                contentDescription = buildString {
                    if (chapter.ordinal != null) append("Day ${chapter.ordinal}, ")
                    append(dateLabel)
                    append(", ")
                    append(counts)
                    if (contributors.isNotEmpty()) {
                        append(", ")
                        append(contributors.joinToString(" and ") { it.displayName.substringBefore(' ') })
                    }
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = numeral,
                style = MaterialTheme.numeralStyle,
                color = extended.numeral,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                )
                Text(
                    text = counts,
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.textMeta,
                )
            }
            AvatarStack(
                users = contributors,
                max = 2,
                size = AvatarSize.Xs,
                ringColor = scheme.surface,
                totalCount = contributors.size,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        // Full bleed, 2 dp, and the same rule the web draws.
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(scheme.onSurface),
        )
    }
}

internal fun <T : ChapterMedia> chapterCounts(chapter: Chapter<T>): String = buildString {
    append(plural(chapter.photoCount, "photo"))
    if (chapter.videoCount > 0) {
        append(" · ")
        append(plural(chapter.videoCount, "video"))
    }
}

internal fun <T : ChapterMedia> compactCounts(chapter: Chapter<T>): String =
    if (chapter.videoCount > 0) {
        "${chapter.photoCount} · ${chapter.videoCount}"
    } else {
        chapter.photoCount.toString()
    }

internal fun plural(count: Int, noun: String) = "$count $noun${if (count == 1) "" else "s"}"

internal val WeekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
internal val MonthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** "Sat 12 Apr" — day-first, the way the web sets it. */
fun formatChapterDate(date: LocalDate): String {
    val weekday = WeekdayNames[date.dayOfWeek.ordinal]
    val month = MonthNames[date.monthNumber - 1]
    return "$weekday ${date.dayOfMonth} $month"
}
