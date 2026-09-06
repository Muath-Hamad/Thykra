package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.LocalArabic
import com.jameeli.thykra.ui.theme.thykra

enum class StampTone { Clay, Warning }

/**
 * Part 2 §4.2. The double-ruled passport mark.
 *
 * Allowed on invite previews, share surfaces and a public Recap header, and never more
 * than one on a screen. It rotates -3 degrees in LTR and +3 in RTL, applied through
 * `graphicsLayer` so the layout bounds stay square and nothing below it shifts.
 */
@Composable
fun Stamp(
    eyebrow: String,
    name: String,
    modifier: Modifier = Modifier,
    tone: StampTone = StampTone.Clay,
) {
    val extended = MaterialTheme.thykra
    val ink = when (tone) {
        StampTone.Clay -> MaterialTheme.colorScheme.tertiary
        StampTone.Warning -> extended.warning
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val arabic = LocalArabic.current
    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .graphicsLayer { rotationZ = if (rtl) 3f else -3f }
            // The second ring, drawn 2 dp outside the first.
            .border(1.dp, ink, shape)
            .padding(2.dp)
            .border(2.dp, ink, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { },
    ) {
        Text(
            text = if (arabic) eyebrow else eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            textAlign = TextAlign.Start,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.8f,
            ),
            color = ink,
        )
    }
}
