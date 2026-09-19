package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.component.YukiSecondaryButton
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.designsystem.theme.YukiSpacing

const val SIGN_OUT_LABEL = "Sign out"

internal const val SIGNED_OUT_TITLE = "Sign in to sync your library"
internal const val SIGNED_OUT_DESCRIPTION =
    "Your installs follow your account, so they are waiting on your next device."
internal const val SIGNED_OUT_ACTION = "Sign in"

@Composable
fun AccountRoute(
    onSignInClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberAvatarPicker(onPicked = viewModel::onAvatarPicked)

    AccountScreen(
        state = state,
        actions = AccountActions(
            onEditAvatarClick = picker::launch,
            onSignInClick = onSignInClick,
            onSignOutClick = viewModel::onSignOut,
            onMessageShown = viewModel::onMessageShown,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class AccountActions(
    val onEditAvatarClick: () -> Unit,
    val onSignInClick: () -> Unit,
    val onSignOutClick: () -> Unit,
    val onMessageShown: () -> Unit,
)

@Composable
internal fun AccountScreen(
    state: AccountState,
    actions: AccountActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val account = state.account

    if (account == null) {
        SignedOut(
            onSignInClick = actions.onSignInClick,
            contentPadding = contentPadding,
            modifier = modifier,
        )
        return
    }

    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect

        hostState.showSnackbar(message = message, withDismissAction = true)
        actions.onMessageShown()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(YukiSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraLarge),
        ) {
            ProfileSection(
                account = account,
                onEditAvatarClick = actions.onEditAvatarClick,
                modifier = Modifier.weight(1f),
            )

            YukiSecondaryButton(
                label = SIGN_OUT_LABEL,
                onClick = actions.onSignOutClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        YukiSnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding),
        )
    }
}

@Composable
private fun SignedOut(
    onSignInClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiScreenCenter(contentPadding = contentPadding, modifier = modifier) {
        CollectionEmpty(
            content = EmptyContent(
                title = SIGNED_OUT_TITLE,
                description = SIGNED_OUT_DESCRIPTION,
                icon = YukiIcons.Person,
                actionLabel = SIGNED_OUT_ACTION,
                onAction = onSignInClick,
            ),
        )
    }
}
