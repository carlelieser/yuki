package app.yuki.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons

const val PASSWORD_ROW_TAG = "passwordRow"

@Composable
internal fun PasswordMethodRow(
    state: SignInMethodsState,
    position: SettingsRowPosition,
    onSetPassword: () -> Unit,
) {
    SettingsRow(
        position = position,
        title = stringResource(R.string.account_password_title),
        supporting = stringResource(
            if (state.hasPassword) R.string.account_password_set else R.string.account_password_not_set,
        ),
        icon = YukiIcons.Key,
        isEnabled = !state.isBusy,
        onClick = if (state.hasPassword) null else onSetPassword,
        modifier = Modifier.testTag(PASSWORD_ROW_TAG),
    )
}
