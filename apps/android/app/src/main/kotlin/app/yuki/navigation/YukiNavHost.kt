package app.yuki.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.yuki.core.designsystem.component.ScreenshotFocus
import app.yuki.core.designsystem.component.ScreenshotFocusScope
import app.yuki.core.designsystem.component.ScreenshotTransitionScope
import app.yuki.feature.explore.ExploreRoute as ExploreScreenRoute
import app.yuki.feature.library.LibraryScreen
import app.yuki.feature.listing.ListingNavigation
import app.yuki.feature.listing.ListingRoute as ListingScreenRoute
import app.yuki.feature.listing.ScreenshotViewerRoute
import app.yuki.feature.search.AuthorRoute as AuthorScreenRoute
import app.yuki.feature.search.CategoryRoute as CategoryScreenRoute
import app.yuki.feature.search.SearchRoute as SearchScreenRoute
import app.yuki.feature.updates.UpdatesScreen
import app.yuki.settings.SettingsDestination
import app.yuki.settings.SettingsNavigationHolder

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun YukiNavHost(
    navController: NavHostController,
    navigator: YukiNavigator,
    navigationHolder: SettingsNavigationHolder,
    bottomBarPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(navigator, navigationHolder) {
        navigationHolder.bind(
            onSignIn = navigator::openSignIn,
            onViewLibrary = { navigator.selectTab(YukiTab.Library) },
        )
    }

    val screenshotFocus = remember { ScreenshotFocus() }

    SharedTransitionLayout(modifier = modifier) {
        ScreenshotFocusScope(focus = screenshotFocus) {
            YukiRoutedContent(
                navController = navController,
                navigator = navigator,
                destinations = DestinationScopes(
                    sharedScope = this@SharedTransitionLayout,
                    bottomBarPadding = bottomBarPadding,
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private class DestinationScopes(
    val sharedScope: SharedTransitionScope,
    val bottomBarPadding: PaddingValues,
)

@Composable
private fun YukiRoutedContent(
    navController: NavHostController,
    navigator: YukiNavigator,
    destinations: DestinationScopes,
) {
    val bottomBarPadding = destinations.bottomBarPadding

    NavHost(
        navController = navController,
        startDestination = ExploreRoute,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        exploreDestination(navigator, bottomBarPadding)
        libraryDestination(navigator, bottomBarPadding)
        updatesDestination(navigator, bottomBarPadding)
        listingDestination(navigator, destinations.sharedScope)
        screenshotDestination(navigator, destinations.sharedScope)
        searchDestination(navigator, bottomBarPadding)
        categoryDestination(navigator, bottomBarPadding)
        authorDestination(navigator, bottomBarPadding)
        settingsDestination(navigator, bottomBarPadding)
        signInDestination(navigator)
        signUpDestination(navigator)
    }
}

private fun NavGraphBuilder.exploreDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<ExploreRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabPopEnter() },
        popExitTransition = { tabPopExit() },
    ) {
        ExploreScreenRoute(
            onListingSelected = navigator::openListing,
            onCategorySelected = navigator::openCategory,
            contentPadding = bottomBarPadding,
            onAuthorSelected = navigator::openAuthor,
            trailing = { AccountButton(onClick = navigator::openSettings) },
        )
    }
}

private fun NavGraphBuilder.libraryDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<LibraryRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabPopEnter() },
        popExitTransition = { tabPopExit() },
    ) {
        LibraryScreen(
            onListingClick = navigator::openListing,
            onExploreClick = { navigator.selectTab(YukiTab.Explore) },
            contentPadding = bottomBarPadding,
        )
    }
}

private fun NavGraphBuilder.updatesDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<UpdatesRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabPopEnter() },
        popExitTransition = { tabPopExit() },
    ) {
        UpdatesScreen(
            onListingClick = navigator::openListing,
            contentPadding = bottomBarPadding,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun NavGraphBuilder.listingDestination(
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
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun NavGraphBuilder.screenshotDestination(
    navigator: YukiNavigator,
    sharedScope: SharedTransitionScope,
) {
    composable<ScreenshotRoute> {
        ScreenshotTransitionScope(sharedScope = sharedScope, contentScope = this) {
            ScreenshotViewerRoute(onBackClick = navigator::navigateUp)
        }
    }
}

private fun NavGraphBuilder.searchDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<SearchRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabPopEnter() },
        popExitTransition = { tabPopExit() },
    ) {
        SearchScreenRoute(
            onListingSelected = navigator::openListing,
            contentPadding = bottomBarPadding,
            onAuthorSelected = navigator::openAuthor,
        )
    }
}

private fun NavGraphBuilder.categoryDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<CategoryRoute> {
        CategoryScreenRoute(
            onListingSelected = navigator::openListing,
            onBackClick = navigator::navigateUp,
            contentPadding = bottomBarPadding,
            onAuthorSelected = navigator::openAuthor,
        )
    }
}

private fun NavGraphBuilder.authorDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<AuthorRoute> {
        AuthorScreenRoute(
            onListingSelected = navigator::openListing,
            onBackClick = navigator::navigateUp,
            contentPadding = bottomBarPadding,
            onAuthorSelected = navigator::openAuthor,
        )
    }
}

private fun NavGraphBuilder.settingsDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<SettingsRoute> {
        SettingsDestination(
            onBackClick = navigator::navigateUp,
            contentPadding = bottomBarPadding,
        )
    }
}
