package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.3. One line of what happened, an avatar, and up to three thumbnails.
 *
 * It sits in a list, so it carries an outline rather than a shadow in either theme.
 * Unseen items — anything newer than the `/activity/seen` marker — tint clay, because
 * new-to-you is about people.
 */
@Composable
fun ActivityCard(
    actor: AvatarUser,
    /** "added 12 photos", "and 2 others reacted to 4 photos", "joined". */
    sentence: String,
    /** "Wadi Rum · 2 h". The trip name is dropped on a per-trip feed. */
    meta: String,
    modifier: Modifier = Modifier,
    thumbnailUrls: List<String> = emptyList(),
    unseen: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val shape = MaterialTheme.shapes.medium

    val line = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = scheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
            ),
        ) {
            append(actor.displayName.substringBefore(' '))
        }
        append(" ")
        append(sentence)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unseen) scheme.tertiaryContainer else scheme.surfaceContainerLow)
            .border(1.dp, scheme.outlineVariant, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$sentence, $meta" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(user = actor, size = AvatarSize.Md)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = extended.textMeta,
            )
        }

        if (thumbnailUrls.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                thumbnailUrls.take(3).forEach { url ->
                    PlainPlateImage(url = url, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}
