package app.yuki

import android.Manifest
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.ProvideKeyboardInsets
import app.yuki.core.designsystem.component.YukiNavBar
import app.yuki.core.designsystem.component.YukiNavBarItem
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.feature.settings.AppearanceMode
import app.yuki.feature.settings.rememberSystemDestinations
import app.yuki.install.InstallFailureActions
import app.yuki.install.InstallFailureSnackbar
import app.yuki.navigation.LaunchRequests
import app.yuki.navigation.YukiNavHost
import app.yuki.navigation.YukiNavigator
import app.yuki.navigation.YukiTab
import app.yuki.navigation.rememberYukiNavigator
import app.yuki.navigation.selectedTab
import app.yuki.navigation.toNavDestination
import app.yuki.notifications.NotificationRationaleDialog
import app.yuki.notifications.rememberNotificationConsent

@Composable
fun YukiApp(
    viewModel: YukiAppViewModel = hiltViewModel(),
    launchRequests: LaunchRequests = remember { LaunchRequests() },
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val settings = theme ?: return

    val isDarkTheme = settings.appearance.resolveIsDarkTheme()

    SystemBarsEffect(isDarkTheme = isDarkTheme)

    YukiTheme(
        isDarkTheme = isDarkTheme,
        isDynamicColorEnabled = settings.isDynamicColorEnabled,
    ) {
        ProvideKeyboardInsets {
            YukiScaffold(
                navController = rememberNavController(),
                viewModel = viewModel,
                launchRequests = launchRequests,
            )
        }
    }
}

@Composable
private fun SystemBarsEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    LaunchedEffect(isDarkTheme) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}

@Composable
private fun AppearanceMode.resolveIsDarkTheme(): Boolean = when (this) {
    AppearanceMode.System -> isSystemInDarkTheme()
    AppearanceMode.Light -> false
    AppearanceMode.Dark -> true
}

@Composable
private fun YukiScaffold(
    navController: NavHostController,
    viewModel: YukiAppViewModel,
    launchRequests: LaunchRequests,
) {
    val consent = rememberNotificationConsent()
    val navigator = rememberYukiNavigator(
        navController = navController,
        onListingOpened = consent.onRequest,
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = backStackEntry?.destination.selectedTab()
    val snackbar = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalYukiSnackbarHostState provides snackbar) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (selectedTab != null) {
                    YukiTabBar(selectedTab = selectedTab, onSelect = navigator::selectTab)
                }
            },
            snackbarHost = { AppSnackbarHost(snackbar, isTabBarShown = selectedTab != null) },
        ) { contentPadding ->
            YukiNavHost(
                navController = navController,
                navigator = navigator,
                navigationHolder = viewModel.settingsNavigation,
                bottomBarPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            )
        }

        InstallFailures(viewModel)
    }

    LaunchRequestsEffect(requests = launchRequests, navigator = navigator)

    NotificationRationaleDialog(state = consent)
}

@Composable
private fun InstallFailures(viewModel: YukiAppViewModel) {
    val failed by viewModel.installFailure.collectAsStateWithLifecycle()
    val destinations = rememberSystemDestinations()

    InstallFailureSnackbar(
        failed = failed,
        actions = InstallFailureActions(
            onRetry = viewModel::onInstallRetry,
            onDismiss = viewModel::onInstallFailureDismissed,
            onAllowInstalls = { shown ->
                destinations.openPermissionSettings(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                viewModel.onInstallFailureDismissed(shown)
            },
        ),
    )
}

@Composable
private fun AppSnackbarHost(hostState: SnackbarHostState, isTabBarShown: Boolean) {
    val insets = if (isTabBarShown) Modifier else Modifier.navigationBarsPadding()

    YukiSnackbarHost(hostState = hostState, modifier = insets)
}

@Composable
private fun LaunchRequestsEffect(requests: LaunchRequests, navigator: YukiNavigator) {
    val requested by requests.requestedTab.collectAsStateWithLifecycle()

    LaunchedEffect(requested) {
        val tab = requested ?: return@LaunchedEffect
        navigator.selectTab(tab)
        requests.consume()
    }
}

@Composable
private fun YukiTabBar(selectedTab: YukiTab, onSelect: (YukiTab) -> Unit) {
    YukiNavBar {
        YukiTab.entries.forEach { tab ->
            YukiNavBarItem(
                destination = tab.toNavDestination(),
                isSelected = tab == selectedTab,
                onSelect = { onSelect(tab) },
            )
        }
    }
}
