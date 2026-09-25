package app.yuki.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.R
import app.yuki.core.designsystem.component.AccountAvatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.feature.account.AccountViewModel

@Composable
internal fun AccountButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account = state.account

    val description = stringResource(
        if (account == null) R.string.app_settings_description else R.string.app_account_description,
    )

    Box(
        modifier = modifier
            .size(YukiSize.MinimumTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (account == null) {
            Icon(
                imageVector = YukiIcons.Settings,
                contentDescription = null,
                modifier = Modifier.size(YukiSize.IconSmall),
            )
            return@Box
        }

        AccountAvatar(
            content = AvatarContent(imageUrl = account.imageUrl, displayName = account.name),
            size = AvatarSize.Compact,
        )
    }
}
