package app.yuki.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.yuki.feature.explore.ExploreRoute as ExploreScreenRoute
import app.yuki.feature.library.LibraryScreen
import app.yuki.feature.listing.ListingNavigation
import app.yuki.feature.listing.ListingRoute as ListingScreenRoute
import app.yuki.feature.listing.ScreenshotViewerRoute
import app.yuki.feature.search.CategoryRoute as CategoryScreenRoute
import app.yuki.feature.search.SearchRoute as SearchScreenRoute
import app.yuki.feature.updates.UpdatesScreen
import app.yuki.settings.SettingsDestination

@Composable
internal fun YukiNavHost(
    navController: NavHostController,
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ExploreRoute,
        modifier = modifier,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        exploreDestination(navigator, bottomBarPadding)
        libraryDestination(navigator, bottomBarPadding)
        updatesDestination(navigator, bottomBarPadding)
        listingDestination(navigator)
        screenshotDestination(navigator)
        searchDestination(navigator, bottomBarPadding)
        categoryDestination(navigator, bottomBarPadding)
        settingsDestination(navigator)
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
            onSettingsClick = navigator::openSettings,
            contentPadding = bottomBarPadding,
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

private fun NavGraphBuilder.listingDestination(navigator: YukiNavigator) {
    composable<ListingRoute>(deepLinks = listingDeepLinks()) { entry ->
        val slug = entry.toRoute<ListingRoute>().slug

        ListingScreenRoute(
            slug = slug,
            navigation = ListingNavigation(
                onBackClick = navigator::navigateUp,
                onScreenshotSelected = { index -> navigator.openScreenshots(slug, index) },
            ),
        )
    }
}

private fun NavGraphBuilder.screenshotDestination(navigator: YukiNavigator) {
    composable<ScreenshotRoute> {
        ScreenshotViewerRoute(onBackClick = navigator::navigateUp)
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
        )
    }
}

private fun NavGraphBuilder.settingsDestination(navigator: YukiNavigator) {
    composable<SettingsRoute> {
        SettingsDestination(onBackClick = navigator::navigateUp)
    }
}
