package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.theme.YukiSpacing

data class SignInNavigation(
    val onSignedIn: () -> Unit,
    val onCreateAccountClick: () -> Unit,
)

@Composable
fun SignInRoute(
    navigation: SignInNavigation,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) navigation.onSignedIn()
    }

    SignInScreen(
        state = state,
        actions = SignInActions(
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::onSubmit,
            onCreateAccountClick = navigation.onCreateAccountClick,
            onMessageShown = viewModel::onMessageShown,
            onResendVerification = viewModel::onResendVerification,
        ),
        modifier = modifier,
    )
}

data class SignInActions(
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onCreateAccountClick: () -> Unit,
    val onMessageShown: () -> Unit,
    val onResendVerification: () -> Unit,
)

@Composable
internal fun SignInScreen(
    state: SignInState,
    actions: SignInActions,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = stringResource(R.string.account_sign_in_title),
        modifier = modifier,
        message = state.authMessage(actions),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        ) {
            EmailField(
                field = CredentialField(
                    value = state.email,
                    error = state.emailError,
                    onValueChange = actions.onEmailChange,
                ),
            )
            PasswordField(
                field = CredentialField(
                    value = state.password,
                    error = state.passwordError,
                    onValueChange = actions.onPasswordChange,
                ),
            )
        }

        YukiButton(
            label = stringResource(R.string.account_sign_in_submit),
            onClick = actions.onSubmit,
            modifier = Modifier.fillMaxWidth(),
            isEnabled = !state.isSubmitting,
        )

        AuthFooterPrompt(
            prompt = stringResource(R.string.account_sign_in_prompt),
            actionLabel = stringResource(R.string.account_sign_in_create),
            onActionClick = actions.onCreateAccountClick,
        )
    }
}

@Composable
private fun SignInState.authMessage(actions: SignInActions): AuthMessage? {
    val text = message?.text() ?: return null
    val resendLabel = stringResource(R.string.account_resend)

    return AuthMessage(
        text = text,
        onShown = actions.onMessageShown,
        action = if (canResendVerification) {
            AuthMessageAction(resendLabel, actions.onResendVerification)
        } else {
            null
        },
    )
}
