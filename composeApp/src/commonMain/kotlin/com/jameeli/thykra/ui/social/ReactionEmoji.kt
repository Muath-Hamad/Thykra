package com.jameeli.thykra.ui.social

import com.jameeli.thykra.model.ReactionType

/**
 * Visual + accessibility metadata for each [ReactionType].
 * Emoji glyphs are used so we don't need to bundle additional asset packs.
 */
object ReactionEmoji {
    fun glyph(type: ReactionType): String = when (type) {
        ReactionType.LOVE -> "❤️" // red heart
        ReactionType.WOW -> "😮" // face with open mouth
        ReactionType.LAUGH -> "😂" // face with tears of joy
        ReactionType.WISH_I_WAS_THERE -> "🙌" // raising hands
        ReactionType.WANDERLUST -> "✈️" // airplane
        ReactionType.BEACH -> "🏖️" // beach with umbrella
        ReactionType.MOUNTAIN -> "⛰️" // mountain
        ReactionType.FOOD -> "🍽️" // fork and knife with plate
    }

    fun label(type: ReactionType): String = when (type) {
        ReactionType.LOVE -> "Love"
        ReactionType.WOW -> "Wow"
        ReactionType.LAUGH -> "Laugh"
        ReactionType.WISH_I_WAS_THERE -> "Wish I was there"
        ReactionType.WANDERLUST -> "Wanderlust"
        ReactionType.BEACH -> "Beach"
        ReactionType.MOUNTAIN -> "Mountain"
        ReactionType.FOOD -> "Food"
    }

    val ordered: List<ReactionType> = listOf(
        ReactionType.LOVE,
        ReactionType.WOW,
        ReactionType.LAUGH,
        ReactionType.WISH_I_WAS_THERE,
        ReactionType.WANDERLUST,
        ReactionType.BEACH,
        ReactionType.MOUNTAIN,
        ReactionType.FOOD
    )
}
