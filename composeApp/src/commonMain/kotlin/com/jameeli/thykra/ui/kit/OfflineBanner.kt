package com.jameeli.thykra.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jameeli.thykra.ui.theme.LocalMotion
import com.jameeli.thykra.ui.theme.ThykraIcons
import com.jameeli.thykra.ui.theme.thykra
import com.jameeli.thykra.ui.theme.thykraTween

/**
 * Part 2 §4.7. A strip under the top bar, not an empty state.
 *
 * Cached content stays interactive beneath it. Actions that need the network do not go
 * grey — they Toast with a Retry, because a disabled button explains nothing.
 */
@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String = "You're offline · showing saved trips",
) {
    val motion = LocalMotion.current
    val extended = MaterialTheme.thykra

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(thykraTween(motion.dur3)) + expandVertically(thykraTween(motion.dur3)),
        exit = fadeOut(thykraTween(motion.dur2)) + shrinkVertically(thykraTween(motion.dur2)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(extended.warningContainer)
                .padding(horizontal = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ThykraIcons.Offline,
                contentDescription = null,
                tint = extended.warning,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = extended.onWarningContainer,
            )
        }
    }
}
