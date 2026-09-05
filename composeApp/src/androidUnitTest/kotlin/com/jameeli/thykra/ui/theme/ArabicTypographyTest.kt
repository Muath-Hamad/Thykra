package com.jameeli.thykra.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jameeli.thykra.chapters.formatOrdinal
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The theme half of the Arabic pass (design part 1, "Arabic typography instance").
 *
 * This covers what the theme itself owes Arabic — the type instance, the display fallback,
 * the tracking rule and the numeral digits. The string table is a separate job and is not
 * what these assert.
 */
class ArabicTypographyTest {

    private val fonts = ThykraFonts(
        display = FontFamily.SansSerif,
        text = FontFamily.Monospace,
    )

    @Test
    fun `arabic display slots fall back to the text family`() {
        val latin = thykraTypography(fonts).typography
        val arabic = thykraTypography(fonts, arabic = true).typography

        // Archivo is Latin-only, so Arabic display has to be set in Readex.
        assertEquals(fonts.display, latin.displayLarge.fontFamily)
        assertEquals(fonts.text, arabic.displayLarge.fontFamily)
        assertEquals(fonts.text, arabic.headlineMedium.fontFamily)

        // Body was always the text family; Arabic changes nothing there.
        assertEquals(fonts.text, latin.bodyMedium.fontFamily)
        assertEquals(fonts.text, arabic.bodyMedium.fontFamily)
    }

    @Test
    fun `arabic display is set smaller and looser`() {
        val latin = thykraTypography(fonts).typography
        val arabic = thykraTypography(fonts, arabic = true).typography

        // 0.92x size, 1.32x line height.
        assertTrue(arabic.displayLarge.fontSize.value < latin.displayLarge.fontSize.value)
        assertTrue(arabic.displayLarge.lineHeight.value > latin.displayLarge.lineHeight.value)
    }

    @Test
    fun `arabic drops every bit of tracking`() {
        val latin = thykraTypography(fonts).typography
        val arabic = thykraTypography(fonts, arabic = true).typography

        assertNotEquals(0f, latin.displayLarge.letterSpacing.value)
        assertEquals(0f, arabic.displayLarge.letterSpacing.value)

        // labelSmall is the one slot with positive tracking in Latin; Arabic loses it,
        // and the Label composable stops uppercasing it.
        assertTrue(latin.labelSmall.letterSpacing.value > 0f)
        assertEquals(0f, arabic.labelSmall.letterSpacing.value)
    }

    @Test
    fun `compact steps the three display slots down and leaves the rest alone`() {
        val regular = thykraTypography(fonts).typography
        val compact = thykraTypography(fonts, compact = true).typography

        assertEquals(44f, regular.displayLarge.fontSize.value)
        assertEquals(34f, compact.displayLarge.fontSize.value)
        assertEquals(28f, compact.displayMedium.fontSize.value)
        assertEquals(23f, compact.displaySmall.fontSize.value)

        // Headlines, body and labels do not move: nothing hides, the display just steps.
        assertEquals(regular.headlineLarge.fontSize.value, compact.headlineLarge.fontSize.value)
        assertEquals(regular.bodyMedium.fontSize.value, compact.bodyMedium.fontSize.value)
        assertEquals(regular.labelLarge.fontSize.value, compact.labelLarge.fontSize.value)
    }

    @Test
    fun `display caps at one and a half times the font scale while body scales fully`() {
        val normal = thykraTypography(fonts, fontScale = 1f).typography
        val doubled = thykraTypography(fonts, fontScale = 2f).typography

        // Sizes are sp, so the platform has already scaled them; the display slot divides
        // the excess back out, which halves its declared value at 2x.
        assertEquals(44f * 0.75f, doubled.displayLarge.fontSize.value, 0.01f)
        // Body and label declare the same size at any scale and scale with the platform.
        assertEquals(normal.bodyMedium.fontSize.value, doubled.bodyMedium.fontSize.value)
        assertEquals(normal.labelLarge.fontSize.value, doubled.labelLarge.fontSize.value)
    }

    @Test
    fun `the numeral is tabular in both scripts and Arabic-Indic only in Arabic`() {
        assertEquals("tnum", thykraTypography(fonts).numeral.fontFeatureSettings)
        assertEquals("tnum", thykraTypography(fonts, arabic = true).numeral.fontFeatureSettings)

        // The chapter numeral is the only place in the app that uses these digits.
        assertEquals("02", formatOrdinal(2))
        assertEquals("٠٢", formatOrdinal(2, arabicIndic = true))
        assertEquals("١١", formatOrdinal(11, arabicIndic = true))
    }

    @Test
    fun `button labels are the text family, not display`() {
        // The single intentional type divergence from web: Archivo Expanded at 15 sp in a
        // 48 dp button eats 25% more width and stops fitting at 360 dp.
        val typography = thykraTypography(fonts).typography
        assertEquals(fonts.text, typography.labelLarge.fontFamily)
        assertEquals(FontWeight.SemiBold, typography.labelLarge.fontWeight)
    }
}

private fun assertEquals(expected: Float, actual: Float, tolerance: Float) {
    assertTrue(
        kotlin.math.abs(expected - actual) <= tolerance,
        "expected $expected but was $actual",
    )
}
