package app.yuki.navigation

import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import app.yuki.BuildConfig

private val storefrontOrigin: String
    get() = BuildConfig.YUKI_BASE_URL.trimEnd('/')

internal fun listingDeepLinks(): List<NavDeepLink> = listOf(
    navDeepLink<ListingRoute>(basePath = "$storefrontOrigin/listings"),
    navDeepLink<ListingRoute>(basePath = "$storefrontOrigin/listings") {
        uriPattern = "$storefrontOrigin/listings/{slug}/download/{tag}"
    },
)
