package app.yuki.navigation

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.AVATAR_OPEN_DESCRIPTION
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

    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = AVATAR_OPEN_DESCRIPTION },
    ) {
        AccountAvatar(
            content = AvatarContent(imageUrl = account?.imageUrl, displayName = account?.name),
            size = AvatarSize.Small,
        )
    }
}
