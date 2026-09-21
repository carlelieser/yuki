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
data object SignInRoute

@Serializable
data object SignUpRoute

@Serializable
data class ListingRoute(val slug: String)

@Serializable
data object SearchRoute

@Serializable
data class CategoryRoute(val category: String)

@Serializable
data class AuthorRoute(val author: String)

@Serializable
data class ScreenshotRoute(
    val slug: String,
    val startIndex: Int,
    val urls: List<String>,
)
