package com.jameeli.thykra.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jameeli.thykra.ui.theme.thykra

/** Part 2 §4.2. The five sizes the kit draws, and nothing between them. */
enum class AvatarSize(val diameter: Int) {
    Xs(24), Sm(32), Md(40), Lg(56), Xl(80);

    /** Initials sit at roughly 0.4x the diameter. */
    val initialsSize: Float get() = diameter * 0.4f
}

/**
 * A person, as a circle.
 *
 * A photo gets a 1 dp ring so it separates from a light background; a tinted fallback
 * does not, because its fill already does that. The tint is hashed from the user id with
 * the same hash the web uses, so a person is the same colour on both platforms.
 */
@Composable
fun Avatar(
    user: AvatarUser,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    /** Set when the surrounding row already names the person. */
    decorative: Boolean = true,
    /** The settings members list marks the owner with a clay dot. */
    ownerBadge: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = MaterialTheme.thykra.avatarTint(user.id)

    Box(
        modifier = modifier
            .size(size.diameter.dp)
            .then(if (decorative) Modifier.clearAndSetSemantics { } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (user.avatarUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(size.diameter.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.initials,
                    color = scheme.onTertiary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = size.initialsSize.sp,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = size.initialsSize.sp,
                        lineHeight = size.initialsSize.sp,
                    ),
                )
            }
        } else {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = if (decorative) null else user.displayName,
                modifier = Modifier
                    .size(size.diameter.dp)
                    .clip(CircleShape)
                    .background(tint)
                    .border(1.dp, scheme.outline, CircleShape),
            )
        }

        if (ownerBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // In RTL, align(BottomEnd) already mirrors to the other corner.
                    .offset(x = 1.dp, y = 1.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .border(1.5.dp, scheme.surface, CircleShape),
            )
        }
    }
}
