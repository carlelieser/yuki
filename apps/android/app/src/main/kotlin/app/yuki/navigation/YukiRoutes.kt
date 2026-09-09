package app.yuki.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ExploreRoute

@Serializable
data object LibraryRoute

@Serializable
data object UpdatesRoute

@Serializable
data object SettingsRoute

@Serializable
data class ListingRoute(val slug: String)
