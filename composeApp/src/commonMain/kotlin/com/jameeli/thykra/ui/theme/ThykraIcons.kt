package com.jameeli.thykra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * The whole icon set — thirty-one hand-drawn paths at a 24 dp viewport, 1.75 dp stroke,
 * round caps and joins, no fills except dots and the three tab glyphs when selected.
 *
 * Icons inherit LocalContentColor through the tint that Icon applies, so nothing here
 * names a colour that survives to the screen. Every Icon call passes a
 * contentDescription, or null when a label sits beside it.
 *
 * Directional icons ([Back], [Chevron]) set autoMirror; nothing else mirrors. [Play],
 * [Volume], [Video] and the tab glyphs never flip.
 *
 * [Alert] does double duty: tinted error it is a failure, tinted warning it is a caution.
 * One path, two colours — the same rule as the web.
 *
 * Deliberately absent: search (there is none in v1), a heart (reactions are the eight
 * emoji, rendered as text), a bin, a door and a block glyph (delete, leave, remove, block
 * and sign out are always text labels, never icons), a drag handle (the sheet draws a
 * 32 x 4 rule itself) and a drop-down caret ([Chevron] rotated 90 degrees).
 */
object ThykraIcons {

    // ── Navigation and chrome ──────────────────────────────────────────────────

    /** Mirrors in RTL. */
    val Back: ImageVector by lazy {
        icon("Back", autoMirror = true) {
            stroke {
                moveTo(20f, 12f); lineTo(4.5f, 12f)
                moveTo(11f, 5.5f); lineTo(4.5f, 12f); lineTo(11f, 18.5f)
            }
        }
    }

    /** Mirrors in RTL. Rotated 90 degrees, it is the drop-down caret. */
    val Chevron: ImageVector by lazy {
        icon("Chevron", autoMirror = true) {
            stroke { moveTo(9.5f, 5f); lineTo(16.5f, 12f); lineTo(9.5f, 19f) }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            stroke {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    /** The overflow sheet trigger. */
    val More: ImageVector by lazy {
        icon("More") {
            solid {
                circle(5.5f, 12f, 1.5f)
                circle(12f, 12f, 1.5f)
                circle(18.5f, 12f, 1.5f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            stroke {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            stroke { moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f) }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroke {
                moveTo(3.5f, 6.5f); lineTo(20.5f, 6.5f)
                moveTo(3.5f, 12f); lineTo(20.5f, 12f)
                moveTo(3.5f, 17.5f); lineTo(20.5f, 17.5f)
                circle(8.8f, 6.5f, 2.1f)
                circle(15.2f, 12f, 2.1f)
                circle(8.8f, 17.5f, 2.1f)
            }
        }
    }

    // ── Sharing ────────────────────────────────────────────────────────────────

    val Share: ImageVector by lazy {
        icon("Share") {
            stroke {
                moveTo(12f, 3.2f); lineTo(12f, 14.6f)
                moveTo(8.2f, 7f); lineTo(12f, 3.2f); lineTo(15.8f, 7f)
                moveTo(8.6f, 9.6f)
                lineTo(6.6f, 9.6f)
                quadTo(4.6f, 9.6f, 4.6f, 11.6f)
                lineTo(4.6f, 18.6f)
                quadTo(4.6f, 20.6f, 6.6f, 20.6f)
                lineTo(17.4f, 20.6f)
                quadTo(19.4f, 20.6f, 19.4f, 18.6f)
                lineTo(19.4f, 11.6f)
                quadTo(19.4f, 9.6f, 17.4f, 9.6f)
                lineTo(15.4f, 9.6f)
            }
        }
    }

    val Link: ImageVector by lazy {
        icon("Link") {
            stroke {
                moveTo(10f, 13f)
                arcToRelative(5f, 5f, 0f, false, false, 7.54f, 0.54f)
                lineToRelative(3f, -3f)
                arcToRelative(5f, 5f, 0f, false, false, -7.07f, -7.07f)
                lineToRelative(-1.72f, 1.71f)
                moveTo(14f, 11f)
                arcToRelative(5f, 5f, 0f, false, false, -7.54f, -0.54f)
                lineToRelative(-3f, 3f)
                arcToRelative(5f, 5f, 0f, false, false, 7.07f, 7.07f)
                lineToRelative(1.71f, -1.71f)
            }
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            stroke {
                roundRect(8.5f, 8.5f, 20.5f, 20.5f, 2.2f)
                moveTo(15.5f, 7.5f)
                lineTo(15.5f, 5f)
                quadTo(15.5f, 3.5f, 14f, 3.5f)
                lineTo(5f, 3.5f)
                quadTo(3.5f, 3.5f, 3.5f, 5f)
                lineTo(3.5f, 14f)
                quadTo(3.5f, 15.5f, 5f, 15.5f)
                lineTo(7.5f, 15.5f)
            }
        }
    }

    // ── People ─────────────────────────────────────────────────────────────────

    val People: ImageVector by lazy {
        icon("People") {
            stroke {
                circle(9f, 9f, 3f)
                moveTo(3.5f, 20f)
                quadTo(3.5f, 14.5f, 9f, 14.5f)
                quadTo(14.5f, 14.5f, 14.5f, 20f)
                circle(16.6f, 9.6f, 2.4f)
                moveTo(16.2f, 14.7f)
                quadTo(20.5f, 15.1f, 20.5f, 20f)
            }
        }
    }

    val PersonAdd: ImageVector by lazy {
        icon("PersonAdd") {
            stroke {
                circle(9.4f, 8.6f, 3.2f)
                moveTo(3.4f, 20.2f)
                quadTo(3.4f, 14.6f, 9.4f, 14.6f)
                quadTo(13.4f, 14.6f, 14.8f, 16.8f)
                moveTo(18.6f, 5.6f); lineTo(18.6f, 11.6f)
                moveTo(15.6f, 8.6f); lineTo(21.6f, 8.6f)
            }
        }
    }

    /** The Me tab draws this with a 2 dp ring instead of a fill, so a real avatar can sit there. */
    val Person: ImageVector by lazy {
        icon("Person") {
            stroke {
                circle(12f, 8.2f, 3.6f)
                moveTo(4.8f, 20.2f)
                quadTo(4.8f, 13.8f, 12f, 13.8f)
                quadTo(19.2f, 13.8f, 19.2f, 20.2f)
            }
        }
    }

    // ── Social ─────────────────────────────────────────────────────────────────

    val Comment: ImageVector by lazy {
        icon("Comment") {
            stroke {
                moveTo(6.5f, 4.5f)
                lineTo(17.5f, 4.5f)
                quadTo(20.5f, 4.5f, 20.5f, 7.5f)
                lineTo(20.5f, 14f)
                quadTo(20.5f, 17f, 17.5f, 17f)
                lineTo(12.6f, 17f)
                lineTo(8f, 20.8f)
                lineTo(8f, 17f)
                lineTo(6.5f, 17f)
                quadTo(3.5f, 17f, 3.5f, 14f)
                lineTo(3.5f, 7.5f)
                quadTo(3.5f, 4.5f, 6.5f, 4.5f)
                close()
            }
        }
    }

    /** Opens the eight-emoji picker. */
    val React: ImageVector by lazy {
        icon("React") {
            stroke {
                circle(12f, 12f, 8.5f)
                moveTo(8.2f, 14.4f)
                quadTo(12f, 17.6f, 15.8f, 14.4f)
            }
            solid {
                circle(9.2f, 10f, 1.15f)
                circle(14.8f, 10f, 1.15f)
            }
        }
    }

    // ── Status ─────────────────────────────────────────────────────────────────

    val Info: ImageVector by lazy {
        icon("Info") {
            stroke {
                circle(12f, 12f, 8.5f)
                moveTo(12f, 11f); lineTo(12f, 16.6f)
            }
            solid { circle(12f, 7.7f, 1.1f) }
        }
    }

    /** Tinted error it is a failure; tinted warning it is a caution. */
    val Alert: ImageVector by lazy {
        icon("Alert") {
            stroke {
                moveTo(10.6f, 4.3f)
                quadTo(12f, 3.2f, 13.4f, 4.3f)
                lineTo(21.4f, 18.6f)
                quadTo(22.2f, 20.4f, 20.2f, 20.4f)
                lineTo(3.8f, 20.4f)
                quadTo(1.8f, 20.4f, 2.6f, 18.6f)
                close()
                moveTo(12f, 9.6f); lineTo(12f, 14.2f)
            }
            solid { circle(12f, 17.3f, 1.05f) }
        }
    }

    val Retry: ImageVector by lazy {
        icon("Retry") {
            stroke {
                moveTo(20.7f, 3.6f); lineTo(20.7f, 9.2f); lineTo(15.1f, 9.2f)
                moveTo(19.6f, 14.6f)
                arcToRelative(8.2f, 8.2f, 0f, true, true, -1.9f, -8.5f)
                lineTo(20.7f, 9.2f)
            }
        }
    }

    /** The connectivity glyph, in the offline banner and its empty state. */
    val Offline: ImageVector by lazy {
        icon("Offline") {
            stroke {
                moveTo(3f, 9.2f); quadTo(12f, 2.2f, 21f, 9.2f)
                moveTo(6.4f, 12.8f); quadTo(12f, 8.4f, 17.6f, 12.8f)
                moveTo(9.6f, 16.2f); quadTo(12f, 14.3f, 14.4f, 16.2f)
                moveTo(4f, 4f); lineTo(20f, 20f)
            }
            solid { circle(12f, 19.4f, 1.15f) }
        }
    }

    val Lock: ImageVector by lazy {
        icon("Lock") {
            stroke {
                moveTo(7.5f, 10.5f)
                lineTo(7.5f, 7.6f)
                quadTo(7.5f, 3.5f, 12f, 3.5f)
                quadTo(16.5f, 3.5f, 16.5f, 7.6f)
                lineTo(16.5f, 10.5f)
                roundRect(4.5f, 10.5f, 19.5f, 20.5f, 2.5f)
            }
        }
    }

    val Globe: ImageVector by lazy {
        icon("Globe") {
            stroke {
                circle(12f, 12f, 8.6f)
                moveTo(3.4f, 12f); lineTo(20.6f, 12f)
                moveTo(12f, 3.4f)
                quadTo(16.4f, 12f, 12f, 20.6f)
                quadTo(7.6f, 12f, 12f, 3.4f)
                close()
            }
        }
    }

    // ── Media ──────────────────────────────────────────────────────────────────

    /** Never mirrors. */
    val Play: ImageVector by lazy {
        icon("Play") {
            stroke { moveTo(8.5f, 5.5f); lineTo(19f, 12f); lineTo(8.5f, 18.5f); close() }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            stroke {
                moveTo(9.5f, 5.5f); lineTo(9.5f, 18.5f)
                moveTo(14.5f, 5.5f); lineTo(14.5f, 18.5f)
            }
        }
    }

    /** Never mirrors. */
    val Volume: ImageVector by lazy {
        icon("Volume") {
            stroke {
                speaker()
                moveTo(14.8f, 9.2f); quadTo(16.7f, 12f, 14.8f, 14.8f)
                moveTo(17.6f, 6.6f); quadTo(21f, 12f, 17.6f, 17.4f)
            }
        }
    }

    val Mute: ImageVector by lazy {
        icon("Mute") {
            stroke {
                speaker()
                moveTo(15.4f, 9.4f); lineTo(21f, 15f)
                moveTo(21f, 9.4f); lineTo(15.4f, 15f)
            }
        }
    }

    /** Never mirrors — the lens always points to the end of the body. */
    val Video: ImageVector by lazy {
        icon("Video") {
            stroke {
                roundRect(2.5f, 6f, 15.5f, 18f, 2.5f)
                moveTo(15.5f, 10.8f)
                lineTo(21.5f, 7.4f)
                lineTo(21.5f, 16.6f)
                lineTo(15.5f, 13.2f)
                close()
            }
        }
    }

    /** The contact sheet. */
    val Grid: ImageVector by lazy {
        icon("Grid") {
            stroke {
                roundRect(3.5f, 3.5f, 10.5f, 10.5f, 1.5f)
                roundRect(13.5f, 3.5f, 20.5f, 10.5f, 1.5f)
                roundRect(3.5f, 13.5f, 10.5f, 20.5f, 1.5f)
                roundRect(13.5f, 13.5f, 20.5f, 20.5f, 1.5f)
            }
        }
    }

    /** Days: the rule, the lead plate and the column beside it. */
    val Chapters: ImageVector by lazy {
        icon("Chapters") {
            stroke {
                moveTo(3.5f, 6.5f); lineTo(20.5f, 6.5f)
                roundRect(3.5f, 9.5f, 12.5f, 20.5f, 1.5f)
                roundRect(14.5f, 9.5f, 20.5f, 20.5f, 1.5f)
            }
        }
    }

    // ── Tabs. Each has a filled twin, used when its tab is selected. ────────────

    val Trips: ImageVector by lazy { icon("Trips") { stroke { mountains() } } }
    val TripsFilled: ImageVector by lazy { icon("TripsFilled") { solid { mountains() } } }

    val Activity: ImageVector by lazy {
        icon("Activity") {
            stroke {
                bell()
                moveTo(10.2f, 19.2f); quadTo(12f, 21f, 13.8f, 19.2f)
            }
        }
    }
    val ActivityFilled: ImageVector by lazy {
        icon("ActivityFilled") {
            solid { bell() }
            stroke { moveTo(10.2f, 19.2f); quadTo(12f, 21f, 13.8f, 19.2f) }
        }
    }

    val Recaps: ImageVector by lazy {
        icon("Recaps") {
            stroke {
                storySegments()
                roundRect(3.5f, 7.5f, 20.5f, 20.2f, 2.5f)
            }
        }
    }
    val RecapsFilled: ImageVector by lazy {
        icon("RecapsFilled") {
            stroke { storySegments() }
            solid { roundRect(3.5f, 7.5f, 20.5f, 20.2f, 2.5f) }
        }
    }
}

// ── Shared sub-paths, so a glyph and its filled twin are the same drawing ───────

private fun PathBuilder.speaker() {
    moveTo(11.5f, 4.5f)
    lineTo(6.6f, 8.8f)
    lineTo(3f, 8.8f)
    lineTo(3f, 15.2f)
    lineTo(6.6f, 15.2f)
    lineTo(11.5f, 19.5f)
    close()
}

private fun PathBuilder.mountains() {
    moveTo(2.5f, 19.8f)
    lineTo(8.2f, 11.4f)
    lineTo(11.6f, 16.4f)
    lineTo(15.2f, 6.6f)
    lineTo(21.5f, 19.8f)
    close()
}

private fun PathBuilder.bell() {
    moveTo(5.5f, 16.6f)
    quadTo(6.6f, 15.4f, 6.6f, 12.6f)
    lineTo(6.6f, 10.6f)
    quadTo(6.6f, 5.6f, 12f, 5.6f)
    quadTo(17.4f, 5.6f, 17.4f, 10.6f)
    lineTo(17.4f, 12.6f)
    quadTo(17.4f, 15.4f, 18.5f, 16.6f)
    close()
}

private fun PathBuilder.storySegments() {
    moveTo(4f, 4.6f); lineTo(9f, 4.6f)
    moveTo(10.2f, 4.6f); lineTo(13.8f, 4.6f)
    moveTo(15f, 4.6f); lineTo(20f, 4.6f)
}

// ── Builder plumbing ───────────────────────────────────────────────────────────

private const val StrokeWidth = 1.75f

private fun icon(
    name: String,
    autoMirror: Boolean = false,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply(block).build()

/**
 * The default. The colour is a placeholder: Icon tints the rendered vector, so it never
 * reaches the screen.
 */
private fun ImageVector.Builder.stroke(pathData: PathBuilder.() -> Unit) = path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = StrokeWidth,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathData,
)

/** Dots, and the three tab glyphs when their tab is selected. */
private fun ImageVector.Builder.solid(pathData: PathBuilder.() -> Unit) = path(
    fill = SolidColor(Color.Black),
    pathBuilder = pathData,
)

/** Four cubic arcs, the usual approximation. */
private const val CircleControl = 0.5523f

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    val k = r * CircleControl
    moveTo(cx, cy - r)
    curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
    curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
    curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
    curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
    close()
}

private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, radius: Float) {
    moveTo(l + radius, t)
    lineTo(r - radius, t)
    quadTo(r, t, r, t + radius)
    lineTo(r, b - radius)
    quadTo(r, b, r - radius, b)
    lineTo(l + radius, b)
    quadTo(l, b, l, b - radius)
    lineTo(l, t + radius)
    quadTo(l, t, l + radius, t)
    close()
}
