package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.chapters.Chapter
import com.jameeli.thykra.chapters.ChapterMedia
import com.jameeli.thykra.chapters.UndatedNumeral
import com.jameeli.thykra.chapters.formatOrdinal
import com.jameeli.thykra.ui.theme.LocalArabic
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.10. The 40 dp bar that pins under the top bar once a chapter header has
 * scrolled past.
 *
 * It is decorative to TalkBack: the heading already exists in the list, and announcing it
 * twice makes the list longer without making it clearer. Tapping it scrolls back to that
 * chapter, which is a shortcut rather than the only way there.
 */
@Composable
fun <T : ChapterMedia> PinnedChapterBar(
    chapter: Chapter<T>,
    modifier: Modifier = Modifier,
    /** 0 while the chapter header is still fully on screen, 1 once it has left. */
    collapseFraction: Float = 1f,
    onClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val arabic = LocalArabic.current

    val numeral = chapter.ordinal?.let { formatOrdinal(it, arabicIndic = arabic) } ?: UndatedNumeral
    val dateLabel = if (chapter.dated) formatChapterDate(chapter.date) else "Added later"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = collapseFraction.coerceIn(0f, 1f)
                // A 4 dp lift as it settles, so the bar arrives rather than appears.
                translationY = -4.dp.toPx() * (1f - collapseFraction.coerceIn(0f, 1f))
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(scheme.surfaceContainerLow)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { invisibleToUser() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$numeral · $dateLabel",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = compactCounts(chapter),
                style = MaterialTheme.typography.titleSmall,
                color = extended.textMeta,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.outlineVariant),
        )
    }
}
