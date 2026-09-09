package app.yuki

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.yuki.core.designsystem.component.YukiNavBar
import app.yuki.core.designsystem.component.YukiNavBarItem
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.navigation.YukiNavHost
import app.yuki.navigation.YukiTab
import app.yuki.navigation.rememberYukiNavigator
import app.yuki.navigation.selectedTab
import app.yuki.navigation.toNavDestination
import app.yuki.notifications.NotificationRationaleDialog
import app.yuki.notifications.rememberNotificationConsent

@Composable
fun YukiApp(viewModel: YukiAppViewModel = hiltViewModel()) {
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()

    YukiTheme(isDynamicColorEnabled = isDynamicColorEnabled) {
        YukiScaffold(navController = rememberNavController())
    }
}

@Composable
private fun YukiScaffold(navController: NavHostController) {
    val consent = rememberNotificationConsent()
    val navigator = rememberYukiNavigator(
        navController = navController,
        onListingOpened = consent.onRequest,
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = backStackEntry?.destination.selectedTab()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (selectedTab != null) {
                YukiTabBar(selectedTab = selectedTab, onSelect = navigator::selectTab)
            }
        },
    ) { contentPadding ->
        YukiNavHost(
            navController = navController,
            navigator = navigator,
            bottomBarPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        )
    }

    NotificationRationaleDialog(state = consent)
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
