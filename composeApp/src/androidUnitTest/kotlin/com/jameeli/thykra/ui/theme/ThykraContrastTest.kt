package com.jameeli.thykra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Design part 1 ends with a contrast table and the claim that everything text-bearing
 * clears AA. This is that claim, executed.
 *
 * Each assertion allows a pair to be *better* than stated but never worse, so re-tuning a
 * token upward does not fail the suite while dropping one below AA does.
 *
 * Three of the document's figures turned out to be optimistic. All three still clear AA
 * comfortably — the tokens are fine and the table is what is wrong:
 *
 * | Pair | Part 1 says | Measures |
 * |---|---|---|
 * | Paper, white on clay | 7.1:1 | 5.8:1 |
 * | Darkroom, ink on blue | 8.1:1 | 7.2:1 |
 * | Darkroom, ink on clay | 6.6:1 | 6.3:1 |
 */
class ThykraContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun assertAtLeast(expected: Double, foreground: Color, background: Color, what: String) {
        val ratio = contrast(foreground, background)
        assertTrue(
            ratio >= expected,
            "$what is ${(ratio * 10).toInt() / 10.0}:1, below the promised $expected:1",
        )
    }

    @Test
    fun `paper body and meta text clear AA`() {
        val s = paperScheme
        assertAtLeast(14.0, s.onSurface, s.surface, "Paper onSurface on surface")
        assertAtLeast(8.0, s.onSurfaceVariant, s.surface, "Paper onSurfaceVariant on surface")
        // 4.9:1 in the design — meta only, never body, and still over AA's 4.5.
        assertAtLeast(4.5, paperExtended.textMeta, s.surface, "Paper textMeta on surface")
    }

    @Test
    fun `paper accents carry their on-colour`() {
        val s = paperScheme
        assertAtLeast(5.0, s.onPrimary, s.primary, "Paper onPrimary on primary")
        assertAtLeast(5.8, s.onTertiary, s.tertiary, "Paper onTertiary on tertiary")
        assertAtLeast(6.5, s.onPrimaryContainer, s.primaryContainer, "Paper tonal blue")
        assertAtLeast(4.5, s.onTertiaryContainer, s.tertiaryContainer, "Paper tonal clay")
    }

    @Test
    fun `darkroom body and meta text clear AA`() {
        val s = darkroomScheme
        assertAtLeast(14.0, s.onSurface, s.surface, "Darkroom onSurface on surface")
        assertAtLeast(6.0, darkroomExtended.textMeta, s.surface, "Darkroom textMeta on surface")
    }

    @Test
    fun `darkroom accents carry their on-colour`() {
        val s = darkroomScheme
        assertAtLeast(7.2, s.onPrimary, s.primary, "Darkroom ink on primary")
        assertAtLeast(6.3, s.onTertiary, s.tertiary, "Darkroom ink on tertiary")
        assertAtLeast(7.0, s.onErrorContainer, s.errorContainer, "Darkroom error container")
    }

    /**
     * The three tokens this change added to the web exist because the old pairing —
     * the solid colour as text on its own soft container — was 4.4:1 in Paper, just
     * under AA. These are the pairs that replaced it.
     */
    @Test
    fun `the added status text tokens clear AA on their own containers`() {
        assertAtLeast(4.5, paperExtended.onSuccessContainer, paperExtended.successContainer, "Paper good-text")
        assertAtLeast(4.5, paperExtended.onWarningContainer, paperExtended.warningContainer, "Paper warn-text")
        assertAtLeast(4.5, paperScheme.onErrorContainer, paperScheme.errorContainer, "Paper bad-text")
        assertAtLeast(4.5, darkroomExtended.onSuccessContainer, darkroomExtended.successContainer, "Darkroom good-text")
        assertAtLeast(4.5, darkroomExtended.onWarningContainer, darkroomExtended.warningContainer, "Darkroom warn-text")
    }

    /** Bone over a photograph, in both themes — the viewer's only text colour. */
    @Test
    fun `onScrim reads over the strong scrim in both themes`() {
        assertAtLeast(7.0, paperExtended.onScrim, Color(0xFF151726), "Paper onScrim over ink")
        assertAtLeast(7.0, darkroomExtended.onScrim, Color(0xFF000000), "Darkroom onScrim over black")
    }

    @Test
    fun `no pure white anywhere except onPrimary and its siblings`() {
        val white = Color(0xFFFFFFFF)
        val paperRoles = listOf(
            paperScheme.surface,
            paperScheme.background,
            paperScheme.surfaceContainer,
            paperScheme.surfaceContainerHigh,
            paperScheme.surfaceContainerHighest,
            paperScheme.surfaceContainerLow,
            paperScheme.surfaceContainerLowest,
            paperScheme.surfaceVariant,
        )
        paperRoles.forEach { role ->
            assertTrue(role != white, "A Paper surface is pure white; the design has none")
        }
        // The exception the design names: white exists only as an on-colour.
        assertEquals(white, paperScheme.onPrimary)
        assertEquals(white, paperScheme.onTertiary)
    }

    /** Darkroom's near-black sits above the OLED smear threshold rather than at zero. */
    @Test
    fun `darkroom surface is not pure black`() {
        assertTrue(darkroomScheme.surface != Color(0xFF000000))
        assertTrue(darkroomScheme.surfaceContainerLowest != Color(0xFF000000))
        // The scrim, and only the scrim, is allowed to be black.
        assertEquals(Color(0xFF000000), darkroomScheme.scrim)
    }

    /** Every role differs between the two schemes, or one of them is wrong. */
    @Test
    fun `the two schemes disagree on every surface and ink`() {
        val differing: List<Pair<String, (ColorScheme) -> Color>> = listOf(
            "primary" to { it.primary },
            "onPrimary" to { it.onPrimary },
            "tertiary" to { it.tertiary },
            "surface" to { it.surface },
            "onSurface" to { it.onSurface },
            "surfaceContainer" to { it.surfaceContainer },
            "outline" to { it.outline },
            "error" to { it.error },
        )
        differing.forEach { (name, role) ->
            assertTrue(
                role(paperScheme) != role(darkroomScheme),
                "$name is the same in Paper and Darkroom",
            )
        }
    }
}
