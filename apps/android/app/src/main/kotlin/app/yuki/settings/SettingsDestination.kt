package app.yuki.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.feature.settings.SettingsScreen

private const val SETTINGS_TITLE = "Settings"

@Composable
fun SettingsDestination(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = SETTINGS_TITLE,
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SettingsScreen()
    }
}
