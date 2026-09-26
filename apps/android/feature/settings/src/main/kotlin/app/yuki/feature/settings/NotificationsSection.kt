package app.yuki.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRowPosition

private const val NOTIFICATION_ROW_COUNT = 1

@Composable
internal fun NotificationsSection(
    preferences: YukiPreferences,
    actions: PreferenceActions,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(
        modifier = modifier,
        label = stringResource(R.string.settings_notifications_title),
    ) {
        ToggleRow(
            position = SettingsRowPosition(index = 0, count = NOTIFICATION_ROW_COUNT),
            content = ToggleContent(
                title = stringResource(R.string.settings_update_notifications_title),
                supporting = stringResource(R.string.settings_update_notifications_supporting),
                isChecked = preferences.isUpdateNotificationEnabled,
            ),
            onCheckedChange = actions.onUpdateNotificationChange,
        )
    }
}
