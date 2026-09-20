package app.yuki.feature.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.Avatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.SettingsSlotRow
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.AuthAccount
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsMessage
import app.yuki.core.settings.api.SettingsGroup as SettingsGroupId
import javax.inject.Inject

const val ACCOUNT_CARD_TAG = "accountCard"
const val SIGN_OUT_ROW_TAG = "signOutRow"
const val PROFILE_ROW_TAG = "profileRow"

private const val EXPANDED_ROTATION = 180f
private const val COLLAPSED_ROTATION = 0f
private const val EXPAND_ROTATION_LABEL = "expandRotation"

internal const val ACCOUNT_SECTION_LABEL = "Account"

const val SIGN_OUT_LABEL = "Sign out"

internal const val SIGNED_OUT_ROW_TITLE = "Sign in"
internal const val SIGNED_OUT_ROW_SUPPORTING = "Sync your library across devices."

class AccountSettingsContributor @Inject constructor(
    private val navigation: AccountSettingsNavigation,
) : SettingsContributor {
    override val group = SettingsGroupId.Account

    @Composable
    override fun Content() {
        val viewModel: AccountViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val account = state.account

        if (account == null) {
            SignedOutCard(onSignInClick = navigation.onSignInClick)
            return
        }

        val picker = rememberAvatarPicker(onPicked = viewModel::onAvatarPicked)

        SettingsMessage(message = state.message, onShown = viewModel::onMessageShown)

        AccountCard(
            account = account,
            isUploadingAvatar = state.isUploadingAvatar,
            onEditAvatarClick = picker::launch,
            onSignOutClick = viewModel::onSignOut,
        )
    }
}

@Composable
private fun SignedOutCard(onSignInClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingsGroup(modifier = modifier.testTag(ACCOUNT_CARD_TAG), label = ACCOUNT_SECTION_LABEL) {
        SettingsRow(
            position = SettingsRowPosition(index = 0, count = 1),
            title = SIGNED_OUT_ROW_TITLE,
            supporting = SIGNED_OUT_ROW_SUPPORTING,
            icon = YukiIcons.Login,
            onClick = onSignInClick,
        )
    }
}

@Composable
private fun AccountCard(
    account: AuthAccount,
    isUploadingAvatar: Boolean,
    onEditAvatarClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rowCount = if (isExpanded) 2 else 1

    SettingsGroup(modifier = modifier.testTag(ACCOUNT_CARD_TAG), label = ACCOUNT_SECTION_LABEL) {
        SettingsSlotRow(
            position = SettingsRowPosition(index = 0, count = rowCount),
            modifier = Modifier.testTag(PROFILE_ROW_TAG),
            onClick = { isExpanded = !isExpanded },
        ) {
            ProfileHeader(
                account = account,
                isUploadingAvatar = isUploadingAvatar,
                isExpanded = isExpanded,
                onEditAvatarClick = onEditAvatarClick,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            SettingsRow(
                position = SettingsRowPosition(index = 1, count = 2),
                title = SIGN_OUT_LABEL,
                icon = YukiIcons.Logout,
                onClick = onSignOutClick,
                modifier = Modifier.testTag(SIGN_OUT_ROW_TAG),
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    account: AuthAccount,
    isUploadingAvatar: Boolean,
    isExpanded: Boolean,
    onEditAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YukiSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            account = account,
            isUploadingAvatar = isUploadingAvatar,
            onEditAvatarClick = onEditAvatarClick,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExpandIndicator(isExpanded = isExpanded)
    }
}

@Composable
private fun ProfileAvatar(
    account: AuthAccount,
    isUploadingAvatar: Boolean,
    onEditAvatarClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        Avatar(
            content = AvatarContent(imageUrl = account.imageUrl, displayName = account.name),
            size = AvatarSize.Medium,
            onEditClick = onEditAvatarClick,
        )

        if (isUploadingAvatar) {
            CircularProgressIndicator(modifier = Modifier.size(YukiSize.IconLarge))
        }
    }
}

@Composable
private fun ExpandIndicator(isExpanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_ROTATION else COLLAPSED_ROTATION,
        label = EXPAND_ROTATION_LABEL,
    )

    Icon(
        imageVector = YukiIcons.ArrowDropDown,
        contentDescription = null,
        modifier = Modifier.rotate(rotation),
    )
}
