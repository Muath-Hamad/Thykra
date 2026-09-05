package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.thykra

/**
 * Part 2 §4.4. One row of a sheet.
 *
 * Destructive rows are drawn in `onErrorContainer` and are always last, so a swipe
 * through the sheet reaches them only after everything safe.
 */
@Composable
fun SheetAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** A second line under the label — "Creates a 7-day link you can share". */
    supporting: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val content = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        destructive -> scheme.onErrorContainer
        else -> scheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (supporting == null) 52.dp else 64.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.thykra.textMeta,
                )
            }
        }
        trailing?.invoke()
    }
}

/** The hairline that separates one action row from the next. */
@Composable
fun SheetDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
