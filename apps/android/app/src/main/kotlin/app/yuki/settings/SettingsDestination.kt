package app.yuki.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.yuki.R
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.feature.settings.SettingsScreen

@Composable
fun SettingsDestination(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsHostViewModel = hiltViewModel()

    YukiDetailScreen(
        title = stringResource(R.string.app_settings_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SettingsScreen(
            contributors = viewModel.contributors,
            contentPadding = contentPadding,
        )
    }
}
