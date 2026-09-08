package app.yuki.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.feature.settings.SettingsScreen

private const val SETTINGS_TITLE = "Settings"
private const val BACK_DESCRIPTION = "Back"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDestination(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = SETTINGS_TITLE) },
                navigationIcon = { BackAction(onBackClick = onBackClick) },
            )
        },
    ) { contentPadding ->
        SettingsScreen(modifier = Modifier.padding(contentPadding))
    }
}

@Composable
private fun BackAction(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(imageVector = YukiIcons.Close, contentDescription = BACK_DESCRIPTION)
    }
}
