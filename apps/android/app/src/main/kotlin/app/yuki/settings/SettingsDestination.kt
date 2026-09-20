package app.yuki.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.feature.settings.SettingsScreen

private const val SETTINGS_TITLE = "Settings"

@Composable
fun SettingsDestination(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsHostViewModel = hiltViewModel()

    YukiDetailScreen(title = SETTINGS_TITLE, onBackClick = onBackClick, modifier = modifier) {
        SettingsScreen(
            contributors = viewModel.contributors,
            contentPadding = contentPadding,
        )
    }
}
