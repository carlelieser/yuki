package app.yuki.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.yuki.core.designsystem.component.ScreenshotTransitionScope
import app.yuki.feature.listing.ListingNavigation
import app.yuki.feature.listing.ListingRoute as ListingScreenRoute
import app.yuki.feature.listing.ReviewComposerRoute as ReviewComposerScreenRoute
import app.yuki.feature.listing.ReviewsRoute as ReviewsScreenRoute
import app.yuki.feature.listing.ScreenshotViewerRoute
import app.yuki.feature.listing.VersionsRoute as VersionsScreenRoute

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun NavGraphBuilder.listingDestination(
    navigator: YukiNavigator,
    sharedScope: SharedTransitionScope,
) {
    composable<ListingRoute>(deepLinks = listingDeepLinks()) { entry ->
        val slug = entry.toRoute<ListingRoute>().slug

        ScreenshotTransitionScope(sharedScope = sharedScope, contentScope = this) {
            ListingScreenRoute(
                slug = slug,
                navigation = ListingNavigation(
                    onBackClick = navigator::navigateUp,
                    onScreenshotSelected = { selection ->
                        navigator.openScreenshots(slug, selection)
                    },
                    onAuthorSelected = navigator::openAuthor,
                    onListingSelected = navigator::openListing,
                    onVersionsSelected = { navigator.openVersions(slug) },
                    onReviewsSelected = { navigator.openReviews(slug) },
                    onWriteReview = { navigator.openReviewComposer(slug) },
                    onSignIn = navigator::openSignIn,
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun NavGraphBuilder.screenshotDestination(
    navigator: YukiNavigator,
    sharedScope: SharedTransitionScope,
) {
    composable<ScreenshotRoute> {
        ScreenshotTransitionScope(sharedScope = sharedScope, contentScope = this) {
            ScreenshotViewerRoute(onBackClick = navigator::navigateUp)
        }
    }
}

internal fun NavGraphBuilder.versionsDestination(navigator: YukiNavigator) {
    composable<VersionsRoute> {
        VersionsScreenRoute(onBackClick = navigator::navigateUp)
    }
}

internal fun NavGraphBuilder.reviewsDestination(navigator: YukiNavigator) {
    composable<ReviewsRoute> {
        ReviewsScreenRoute(onBackClick = navigator::navigateUp)
    }
}

internal fun NavGraphBuilder.reviewComposerDestination(navigator: YukiNavigator) {
    composable<ReviewComposerRoute> {
        ReviewComposerScreenRoute(onDone = navigator::navigateUp)
    }
}
