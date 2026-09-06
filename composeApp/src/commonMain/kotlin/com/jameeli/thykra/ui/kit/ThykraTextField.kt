package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.6.
 *
 * A filled field on the sunken container, small radius on all four corners and no
 * indicator line — focus is a 2 dp inner stroke instead. The label sits **above** the
 * field and never floats: a floating label breaks at 200% type, which is a size this app
 * has to work at.
 */
@Composable
fun ThykraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    /** "Friends will see this" — replaced by [error] when one is set. */
    helper: String? = null,
    error: String? = null,
    /** Draws "15 / 60" at the end of the helper row and caps input at `maxLength`. */
    maxLength: Int? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    val hasError = error != null

    val strokeColor = when {
        hasError -> scheme.error
        focused -> scheme.primary
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (hasError) scheme.error else extended.textMeta,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 52.dp else 52.dp * minLines)
                .background(scheme.surfaceContainerHighest, shape)
                .then(
                    if (strokeColor != null) Modifier.border(2.dp, strokeColor, shape) else Modifier,
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = { next ->
                        onValueChange(if (maxLength != null) next.take(maxLength) else next)
                    },
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    interactionSource = interactionSource,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    ),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = keyboardType,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                )
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = extended.textMeta,
                        modifier = Modifier.padding(vertical = 14.dp),
                    )
                }
            }
            trailing?.invoke()
        }

        if (error != null || helper != null || maxLength != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (error != null) {
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (error != null) {
                        Icon(
                            imageVector = ThykraIcons.Alert,
                            contentDescription = null,
                            tint = scheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = error ?: helper.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error != null) scheme.error else extended.textMeta,
                    )
                }
                if (maxLength != null) {
                    Text(
                        // Western digits even in Arabic — a counter is a measurement.
                        text = "${value.length} / $maxLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textMeta,
                    )
                }
            }
        }
    }
}
