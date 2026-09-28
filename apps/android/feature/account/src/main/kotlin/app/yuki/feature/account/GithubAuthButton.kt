package app.yuki.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.YukiSecondaryButton

const val GITHUB_AUTH_BUTTON_TAG = "githubAuthButton"

@Composable
internal fun GithubAuthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    YukiSecondaryButton(
        label = stringResource(R.string.account_github_continue),
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(GITHUB_AUTH_BUTTON_TAG),
        isEnabled = isEnabled,
    )
}
