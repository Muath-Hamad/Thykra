package com.jameeli.thykra.chapters

import com.jameeli.thykra.model.MediaType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The same cases as the web's `chapters.test.ts`, so the two implementations stay one
 * grouping rather than two. All instants are UTC and the tests pin the timezone, so a CI
 * box in another zone gets the same answer.
 */
class ChaptersTest {

    private data class Media(
        override val id: String,
        override val type: MediaType = MediaType.PHOTO,
        override val takenAt: Instant? = null,
        override val uploadedAt: Instant,
        override val width: Int? = null,
        override val height: Int? = null,
    ) : ChapterMedia, HasDimensions

    private val utc = TimeZone.UTC

    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun `rule 1 - sorts by takenAt or uploadedAt and groups by local date`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("b", takenAt = at("2026-04-12T09:00:00Z"), uploadedAt = at("2026-04-14T00:00:00Z")),
                Media("a", takenAt = at("2026-04-11T18:00:00Z"), uploadedAt = at("2026-04-14T00:00:00Z")),
                Media("c", takenAt = at("2026-04-12T21:00:00Z"), uploadedAt = at("2026-04-14T00:00:00Z")),
            ),
            utc,
        )

        assertEquals(2, chapters.size)
        assertEquals(LocalDate(2026, 4, 11), chapters[0].date)
        assertEquals(listOf("a"), chapters[0].items.map { it.id })
        assertEquals(listOf("b", "c"), chapters[1].items.map { it.id })
    }

    @Test
    fun `rule 2 - ordinals are ordinal within the trip, gaps make no empty chapters`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("a", takenAt = at("2026-04-11T10:00:00Z"), uploadedAt = at("2026-04-11T10:00:00Z")),
                Media("b", takenAt = at("2026-04-15T10:00:00Z"), uploadedAt = at("2026-04-15T10:00:00Z")),
            ),
            utc,
        )

        assertEquals(listOf(1, 2), chapters.map { it.ordinal })
        assertEquals(listOf("d:2026-04-11", "d:2026-04-15"), chapters.map { it.key })
    }

    @Test
    fun `rule 3 - no takenAt groups by uploadedAt, no numeral, sorted after dated`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("undated", uploadedAt = at("2026-04-10T10:00:00Z")),
                Media("dated", takenAt = at("2026-04-12T10:00:00Z"), uploadedAt = at("2026-04-12T10:00:00Z")),
            ),
            utc,
        )

        assertEquals(2, chapters.size)
        assertTrue(chapters[0].dated)
        assertEquals(1, chapters[0].ordinal)
        // The undated chapter sorts last even though it was uploaded first.
        assertTrue(!chapters[1].dated)
        assertNull(chapters[1].ordinal)
        assertEquals("u:2026-04-10", chapters[1].key)
    }

    @Test
    fun `rule 4 - single-date trip renders one chapter with the numeral suppressed`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("a", takenAt = at("2026-04-12T08:00:00Z"), uploadedAt = at("2026-04-12T08:00:00Z")),
                Media("b", takenAt = at("2026-04-12T19:00:00Z"), uploadedAt = at("2026-04-12T19:00:00Z")),
            ),
            utc,
        )

        assertEquals(1, chapters.size)
        assertNull(chapters[0].ordinal)
    }

    @Test
    fun `rule 4 counter-case - one dated chapter plus an undated one keeps the numeral`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("a", takenAt = at("2026-04-12T08:00:00Z"), uploadedAt = at("2026-04-12T08:00:00Z")),
                Media("b", uploadedAt = at("2026-04-14T08:00:00Z")),
            ),
            utc,
        )

        assertEquals(2, chapters.size)
        assertEquals(1, chapters[0].ordinal)
    }

    @Test
    fun `rule 5 - the lead is the earliest item of the day`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("late", takenAt = at("2026-04-12T21:00:00Z"), uploadedAt = at("2026-04-12T21:00:00Z")),
                Media("early", takenAt = at("2026-04-12T06:00:00Z"), uploadedAt = at("2026-04-12T06:00:00Z")),
            ),
            utc,
        )

        assertEquals("early", chapters[0].lead.id)
        assertEquals(listOf("late"), chapters[0].rest.map { it.id })
    }

    @Test
    fun `counts photos and videos per chapter`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("p1", takenAt = at("2026-04-12T06:00:00Z"), uploadedAt = at("2026-04-12T06:00:00Z")),
                Media("v1", MediaType.VIDEO, at("2026-04-12T07:00:00Z"), at("2026-04-12T07:00:00Z")),
                Media("p2", takenAt = at("2026-04-12T08:00:00Z"), uploadedAt = at("2026-04-12T08:00:00Z")),
            ),
            utc,
        )

        assertEquals(2, chapters[0].photoCount)
        assertEquals(1, chapters[0].videoCount)
    }

    @Test
    fun `chapterOfMedia finds the chapter holding an id`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("a", takenAt = at("2026-04-11T10:00:00Z"), uploadedAt = at("2026-04-11T10:00:00Z")),
                Media("b", takenAt = at("2026-04-12T10:00:00Z"), uploadedAt = at("2026-04-12T10:00:00Z")),
            ),
            utc,
        )

        assertEquals("d:2026-04-12", chapters.chapterOfMedia("b")?.key)
        assertNull(chapters.chapterOfMedia("missing"))
    }

    @Test
    fun `heroCandidate is the largest-area photo, and never a video`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("small", takenAt = at("2026-04-12T06:00:00Z"), uploadedAt = at("2026-04-12T06:00:00Z"), width = 800, height = 600),
                Media("video", MediaType.VIDEO, at("2026-04-12T07:00:00Z"), at("2026-04-12T07:00:00Z"), width = 4000, height = 3000),
                Media("big", takenAt = at("2026-04-12T08:00:00Z"), uploadedAt = at("2026-04-12T08:00:00Z"), width = 4032, height = 3024),
            ),
            utc,
        )

        assertEquals("small", chapters[0].lead.id)
        assertEquals("big", chapters[0].heroCandidate.id)
    }

    @Test
    fun `heroCandidate falls back to the lead when nothing carries dimensions`() {
        val chapters = groupIntoChapters(
            listOf(
                Media("a", takenAt = at("2026-04-12T06:00:00Z"), uploadedAt = at("2026-04-12T06:00:00Z")),
                Media("b", takenAt = at("2026-04-12T08:00:00Z"), uploadedAt = at("2026-04-12T08:00:00Z")),
            ),
            utc,
        )

        assertEquals("a", chapters[0].heroCandidate.id)
    }

    @Test
    fun `formatOrdinal pads to two digits and can render Arabic-Indic`() {
        assertEquals("01", formatOrdinal(1))
        assertEquals("11", formatOrdinal(11))
        assertEquals("٠٢", formatOrdinal(2, arabicIndic = true))
        assertEquals("١٢", formatOrdinal(12, arabicIndic = true))
    }

    @Test
    fun `localDateOf returns the viewer-local calendar date`() {
        // 23:30 UTC on the 12th is the 13th in Amman (UTC+3).
        val instant = at("2026-04-12T23:30:00Z")
        assertEquals(LocalDate(2026, 4, 12), localDateOf(instant, TimeZone.UTC))
        assertEquals(LocalDate(2026, 4, 13), localDateOf(instant, TimeZone.of("Asia/Amman")))
    }
}
