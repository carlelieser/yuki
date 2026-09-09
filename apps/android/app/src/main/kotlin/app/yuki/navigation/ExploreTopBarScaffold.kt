package app.yuki.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

internal const val SETTINGS_ACTION_TAG = "explore_settings_action"

private const val EXPLORE_TITLE = "Yuki"
private const val SETTINGS_DESCRIPTION = "Settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreTopBarScaffold(
    onSettingsClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = EXPLORE_TITLE) },
                actions = { SettingsAction(onSettingsClick = onSettingsClick) },
            )
        },
        content = content,
    )
}

@Composable
private fun SettingsAction(onSettingsClick: () -> Unit) {
    IconButton(
        onClick = onSettingsClick,
        modifier = Modifier.testTag(SETTINGS_ACTION_TAG),
    ) {
        Icon(
            imageVector = YukiTabIcons.Settings,
            contentDescription = SETTINGS_DESCRIPTION,
        )
    }
}
