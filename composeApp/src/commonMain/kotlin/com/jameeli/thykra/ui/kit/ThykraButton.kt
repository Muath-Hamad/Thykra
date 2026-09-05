package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.1.
 *
 * Six variants, one height, one shape. Blue is what the system does; clay is what people
 * do — which is why [ThykraButtonVariant.People] is allowed only on Join, Invite friends
 * and "Add to my trips" on a Recap, and why Add photos is [ThykraButtonVariant.Filled]
 * blue despite being about photographs.
 *
 * A destructive *filled* button exists only inside [ConfirmDialog]; `destructive` here
 * colours a [ThykraButtonVariant.Text] button and nothing else.
 */
enum class ThykraButtonVariant { Filled, Tonal, Outlined, Text, People, PeopleTonal }

/** Height 48, or 40 for the dense rows inside a sheet. */
enum class ThykraButtonSize(val height: Int) { Regular(48), Compact(40) }

/** A button described rather than drawn, so [EmptyState] can take one as data. */
@androidx.compose.runtime.Immutable
data class ThykraButtonSpec(
    val label: String,
    val onClick: () -> Unit,
    val variant: ThykraButtonVariant = ThykraButtonVariant.Filled,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

@Composable
fun ThykraButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ThykraButtonVariant = ThykraButtonVariant.Filled,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    /**
     * A Text button turns error-coloured. On a Filled button this paints error/onError —
     * which only [ConfirmDialog] may ask for, because a filled destructive button exists
     * nowhere else in the app.
     */
    destructive: Boolean = false,
    size: ThykraButtonSize = ThykraButtonSize.Regular,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.thykra
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val shape = MaterialTheme.shapes.small
    val contentPadding = PaddingValues(
        horizontal = if (variant == ThykraButtonVariant.Text) 12.dp else 20.dp,
        vertical = 0.dp,
    )
    val buttonModifier = modifier
        .height(size.height.dp)
        .defaultMinSize(minWidth = 64.dp)
    // Loading keeps the measured width: the label stays, the icon slot holds the spinner.
    val isEnabled = enabled && !loading

    val content: @Composable () -> Unit = {
        ButtonContent(label = label, icon = icon, loading = loading)
    }

    when (variant) {
        ThykraButtonVariant.Filled, ThykraButtonVariant.People -> {
            val people = variant == ThykraButtonVariant.People
            val container = when {
                destructive -> scheme.error
                people && pressed -> extended.tertiaryPressed
                people -> scheme.tertiary
                pressed -> extended.primaryPressed
                else -> scheme.primary
            }
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = when {
                        destructive -> scheme.onError
                        people -> scheme.onTertiary
                        else -> scheme.onPrimary
                    },
                ),
                // Every kit part picks its container explicitly so Material never paints
                // a tonal tint of its own on top.
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                content = { content() },
            )
        }

        ThykraButtonVariant.Tonal, ThykraButtonVariant.PeopleTonal -> {
            val people = variant == ThykraButtonVariant.PeopleTonal
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = shape,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (people) scheme.tertiaryContainer else scheme.primaryContainer,
                    contentColor = if (people) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                content = { content() },
            )
        }

        ThykraButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = isEnabled,
            shape = shape,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface),
            content = { content() },
        )

        ThykraButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = isEnabled,
            shape = shape,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (destructive) scheme.error else scheme.onSurface,
            ),
            content = { content() },
        )
    }
}

@Composable
private fun ButtonContent(label: String, icon: ImageVector?, loading: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = if (loading) {
            // TalkBack hears the label plus its progress rather than a silent disable.
            Modifier.semantics { stateDescription = "In progress" }
        } else {
            Modifier
        },
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )

            icon != null -> Icon(
                imageVector = icon,
                // The label is right beside it, so the icon adds nothing to announce.
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            // Never ellipsis: below 360 dp the parent wraps to a column instead.
            overflow = TextOverflow.Clip,
        )
    }
}
