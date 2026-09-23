package app.yuki.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons

internal data class InstallSourceActions(
    val onSelect: (String) -> Unit,
    val onChooseApp: () -> Unit,
)

internal data class InstallSourceMenuState(
    val isExpanded: Boolean,
    val isPlayStoreInstalled: Boolean,
)

@Composable
internal fun InstallSourceRow(
    position: SettingsRowPosition,
    source: InstallSourceSelection,
    actions: InstallSourceActions,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val collapse = { isExpanded = false }

    SettingsRow(
        position = position,
        title = INSTALL_SOURCE_TITLE,
        supporting = source.label,
        onClick = { isExpanded = true },
        trailing = {
            InstallSourceSelector(
                state = InstallSourceMenuState(isExpanded, source.isPlayStoreInstalled),
                onExpandedChange = { expanded -> isExpanded = expanded },
                actions = collapsingActions(actions, collapse),
            )
        },
    )
}

private fun collapsingActions(
    actions: InstallSourceActions,
    collapse: () -> Unit,
): InstallSourceActions = InstallSourceActions(
    onSelect = { packageName ->
        collapse()
        actions.onSelect(packageName)
    },
    onChooseApp = {
        collapse()
        actions.onChooseApp()
    },
)

@Composable
private fun InstallSourceSelector(
    state: InstallSourceMenuState,
    onExpandedChange: (Boolean) -> Unit,
    actions: InstallSourceActions,
) {
    Box {
        IconButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.testTag(INSTALL_SOURCE_SELECTOR_TAG),
        ) {
            Icon(
                imageVector = YukiIcons.ArrowDropDown,
                contentDescription = INSTALL_SOURCE_TITLE,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = state.isExpanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            PresetItems(
                isPlayStoreInstalled = state.isPlayStoreInstalled,
                onSelect = actions.onSelect,
            )

            DropdownMenuItem(
                text = { Text(text = INSTALL_SOURCE_CHOOSE) },
                onClick = actions.onChooseApp,
            )
        }
    }
}

@Composable
private fun PresetItems(isPlayStoreInstalled: Boolean, onSelect: (String) -> Unit) {
    INSTALL_SOURCE_PRESETS.forEach { preset ->
        val isPlayStore = preset.packageName == PLAY_STORE_PACKAGE

        DropdownMenuItem(
            text = { Text(text = preset.label) },
            enabled = !isPlayStore || isPlayStoreInstalled,
            onClick = { onSelect(preset.packageName) },
        )
    }
}
