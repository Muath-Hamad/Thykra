package com.jameeli.thykra.ui.kit

import androidx.compose.runtime.Immutable
import com.jameeli.thykra.model.AlbumMemberDto
import com.jameeli.thykra.model.AlbumMemberSummary
import com.jameeli.thykra.model.ActivityActorDto
import com.jameeli.thykra.model.InviterDto
import com.jameeli.thykra.model.UserDto

/**
 * The one shape every kit part that draws a person takes.
 *
 * The app has five different DTOs for "a person" depending on which endpoint answered.
 * Rather than overload each kit part five ways, they all narrow to this, and the
 * adapters below do the narrowing at the call site.
 */
@Immutable
data class AvatarUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
) {
    /**
     * First letter of the first two words; one letter for a single-word name. Empty
     * names fall back to a dot rather than an empty circle.
     */
    val initials: String
        get() {
            val words = displayName.trim().split(' ').filter { it.isNotBlank() }
            return when (words.size) {
                0 -> "·"
                1 -> words[0].take(1).uppercase()
                else -> (words[0].take(1) + words[1].take(1)).uppercase()
            }
        }
}

fun UserDto.toAvatarUser() = AvatarUser(id, displayName, avatarUrl)
fun AlbumMemberSummary.toAvatarUser() = AvatarUser(userId, displayName, avatarUrl)
fun AlbumMemberDto.toAvatarUser() = AvatarUser(userId, displayName, avatarUrl)
fun ActivityActorDto.toAvatarUser() = AvatarUser(userId, displayName, avatarUrl)

/**
 * The inviter has no user id on the wire, so the avatar tint is hashed from the name
 * instead. The same person still lands on the same colour on both platforms, because the
 * web does the same thing on the same field.
 */
fun InviterDto.toAvatarUser() = AvatarUser(displayName, displayName, avatarUrl)
