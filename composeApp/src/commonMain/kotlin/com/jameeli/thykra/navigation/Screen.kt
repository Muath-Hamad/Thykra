package com.jameeli.thykra.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginScreen

@Serializable
object HomeScreen

@Serializable
object ProfileScreen

@Serializable
object AlbumListScreen

@Serializable
data class AlbumDetailScreen(val albumId: String)

@Serializable
data class MediaViewerScreen(val albumId: String, val initialMediaId: String)

/**
 * The kit gallery — build step 02's checklist, drawn.
 *
 * Registered only when [com.jameeli.thykra.KitGalleryEnabled] is true, which is a debug
 * build. It is not a product surface: nothing in the app navigates to it, and it never
 * appears in a release binary.
 */
@Serializable
object KitGallery
