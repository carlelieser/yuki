package app.yuki.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.AccountAvatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.feature.account.AccountViewModel

@Composable
internal fun AccountButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account

    AccountAvatar(
        content = AvatarContent(imageUrl = account?.imageUrl, displayName = account?.name),
        onClick = onClick,
        size = AvatarSize.Small,
        modifier = modifier,
    )
}
