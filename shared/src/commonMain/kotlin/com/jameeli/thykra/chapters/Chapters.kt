package com.jameeli.thykra.chapters

import com.jameeli.thykra.model.MediaType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Day-chapter grouping — the Kotlin side of the web's `chapters.ts`, rule for rule, so a
 * trip breaks into the same days on a phone as it does in a browser.
 *
 * 1. Sort all media by `takenAt ?: uploadedAt` ascending; group by calendar date in the
 *    viewer's timezone.
 * 2. Day numerals are ordinal within the trip — the first day with any media is 01,
 *    regardless of gaps. A three-day gap does not create empty chapters.
 * 3. Items with no `takenAt` group by `uploadedAt`, read as "Added later" with no
 *    numeral, and sort after every dated chapter.
 * 4. A trip whose media all share one date renders exactly one chapter and the numeral
 *    is suppressed.
 * 5. [Chapter.lead] is the earliest item of the day.
 */

/** The structural shape the grouping needs — satisfied by `MediaDto` and `PublicMediaDto`. */
interface ChapterMedia {
    val id: String
    val type: MediaType
    val takenAt: Instant?
    val uploadedAt: Instant
}

data class Chapter<T : ChapterMedia>(
    /** Stable key: `d:2026-04-12` for dated, `u:2026-04-14` for undated. */
    val key: String,
    /** 1-based ordinal within the trip; null for undated chapters and for rule 4. */
    val ordinal: Int?,
    /** Local calendar date of the chapter in the viewer's timezone. */
    val date: LocalDate,
    /** True when the group came from `takenAt`; false when from `uploadedAt`. */
    val dated: Boolean,
    /** Earliest item of the day. */
    val lead: T,
    /** The rest of the day's items, ascending. */
    val rest: List<T>,
    /** All items, ascending. */
    val items: List<T>,
    val photoCount: Int,
    val videoCount: Int,
) {
    /**
     * The plate the mobile grid runs full-span at the top of the chapter: the
     * largest-area photo of the day, falling back to [lead] when nothing carries
     * dimensions.
     *
     * This is where mobile and web diverge on purpose. The web's justified row wants the
     * earliest item first because the row reads left to right in time; a staggered grid
     * has no such reading order, so part 2 §4.10 asks for the biggest photograph instead.
     * The grouping itself — which is what has to match — is identical.
     */
    val heroCandidate: T get() = items
        .filter { it.type == MediaType.PHOTO }
        .maxByOrNull { areaOf(it) }
        ?: lead
}

private fun areaOf(media: ChapterMedia): Long {
    val dimensions = media as? HasDimensions ?: return 0L
    val width = dimensions.width ?: return 0L
    val height = dimensions.height ?: return 0L
    return width.toLong() * height.toLong()
}

/** Implemented by media types that carry pixel dimensions. */
interface HasDimensions {
    val width: Int?
    val height: Int?
}

fun ChapterMedia.effectiveInstant(): Instant = takenAt ?: uploadedAt

fun localDateOf(instant: Instant, timeZone: TimeZone): LocalDate =
    instant.toLocalDateTime(timeZone).date

fun <T : ChapterMedia> groupIntoChapters(
    media: List<T>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<Chapter<T>> {
    val sorted = media.sortedWith(
        compareBy({ it.effectiveInstant() }, { it.id }),
    )

    val dated = LinkedHashMap<LocalDate, MutableList<T>>()
    val undated = LinkedHashMap<LocalDate, MutableList<T>>()
    for (item in sorted) {
        val takenAt = item.takenAt
        if (takenAt != null) {
            dated.getOrPut(localDateOf(takenAt, timeZone)) { mutableListOf() }.add(item)
        } else {
            undated.getOrPut(localDateOf(item.uploadedAt, timeZone)) { mutableListOf() }.add(item)
        }
    }

    val datedKeys = dated.keys.sorted()
    val undatedKeys = undated.keys.sorted()

    // Rule 4: one dated chapter and nothing undated means the numeral says nothing.
    val suppressNumeral = datedKeys.size == 1 && undatedKeys.isEmpty()

    return buildList {
        datedKeys.forEachIndexed { index, date ->
            add(
                chapterOf(
                    key = "d:$date",
                    ordinal = if (suppressNumeral) null else index + 1,
                    date = date,
                    dated = true,
                    items = dated.getValue(date),
                ),
            )
        }
        undatedKeys.forEach { date ->
            add(
                chapterOf(
                    key = "u:$date",
                    ordinal = null,
                    date = date,
                    dated = false,
                    items = undated.getValue(date),
                ),
            )
        }
    }
}

private fun <T : ChapterMedia> chapterOf(
    key: String,
    ordinal: Int?,
    date: LocalDate,
    dated: Boolean,
    items: List<T>,
) = Chapter(
    key = key,
    ordinal = ordinal,
    date = date,
    dated = dated,
    lead = items.first(),
    rest = items.drop(1),
    items = items,
    photoCount = items.count { it.type == MediaType.PHOTO },
    videoCount = items.count { it.type == MediaType.VIDEO },
)

/** The chapter holding a media id — for "scroll to Day 2" and for the viewer's position pill. */
fun <T : ChapterMedia> List<Chapter<T>>.chapterOfMedia(mediaId: String): Chapter<T>? =
    firstOrNull { chapter -> chapter.items.any { it.id == mediaId } }

private val ArabicIndicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

/**
 * Two-digit day numeral. Arabic-Indic digits when the app is typesetting Arabic — the
 * chapter numeral is the only place in the app that uses them; every other number stays
 * Western.
 */
fun formatOrdinal(ordinal: Int, arabicIndic: Boolean = false): String {
    val padded = ordinal.toString().padStart(2, '0')
    if (!arabicIndic) return padded
    return padded.map { ArabicIndicDigits[it - '0'] }.joinToString("")
}

/** The numeral an undated chapter shows in place of a number. */
const val UndatedNumeral = "—"
