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
