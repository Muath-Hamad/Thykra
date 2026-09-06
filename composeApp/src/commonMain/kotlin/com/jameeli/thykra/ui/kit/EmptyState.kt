package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.LocalArabic
import com.jameeli.thykra.ui.theme.PlateShape
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/** Three drawn shapes, and no illustration. */
enum class EmptyGlyph { Plate, Person, Offline }

/**
 * Part 2 §4.7.
 *
 * A headline with one clay phrase in it, a line of body, a glyph and at most two buttons.
 * Copy lives at the call site because it differs per surface; [clayPhrase] builds the
 * headline so the emphasis is drawn the same way everywhere.
 */
@Composable
fun EmptyState(
    headline: AnnotatedString,
    body: String,
    modifier: Modifier = Modifier,
    glyph: EmptyGlyph = EmptyGlyph.Plate,
    primary: ThykraButtonSpec? = null,
    secondary: ThykraButtonSpec? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmptyGlyphShape(glyph)

        Text(
            text = headline,
            style = MaterialTheme.typography.displayMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = extended.textMeta,
            textAlign = TextAlign.Center,
        )

        if (primary != null || secondary != null) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                primary?.let {
                    ThykraButton(
                        label = it.label,
                        onClick = it.onClick,
                        variant = it.variant,
                        icon = it.icon,
                        enabled = it.enabled,
                    )
                }
                secondary?.let {
                    ThykraButton(
                        label = it.label,
                        onClick = it.onClick,
                        variant = it.variant,
                        icon = it.icon,
                        enabled = it.enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGlyphShape(glyph: EmptyGlyph) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    when (glyph) {
        EmptyGlyph.Plate -> Box(
            modifier = Modifier
                .size(width = 84.dp, height = 60.dp)
                .background(scheme.surfaceVariant, PlateShape)
                .border(1.dp, extended.plateOutline, PlateShape),
        )

        EmptyGlyph.Person -> Box(
            modifier = Modifier
                .size(60.dp)
                .border(1.dp, scheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ThykraIcons.Person,
                contentDescription = null,
                tint = extended.textMeta,
                modifier = Modifier.size(28.dp),
            )
        }

        EmptyGlyph.Offline -> Icon(
            imageVector = ThykraIcons.Offline,
            contentDescription = null,
            tint = extended.warning,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * A headline with one clay phrase in it — italic in English, weight instead of italic in
 * Arabic, because Readex has no italic and a faux slant on Arabic is wrong.
 */
@Composable
fun clayPhrase(before: String, phrase: String, after: String = ""): AnnotatedString {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val arabic = LocalArabic.current
    return buildAnnotatedString {
        append(before)
        withStyle(
            SpanStyle(
                color = tertiary,
                fontStyle = if (arabic) FontStyle.Normal else FontStyle.Italic,
                fontWeight = if (arabic) FontWeight.Bold else null,
            ),
        ) {
            append(phrase)
        }
        append(after)
    }
}
