package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.theme.YukiSpacing

const val SIGN_IN_SUBMIT_LABEL = "Sign in"

internal const val SIGN_IN_TITLE = "Sign in to Yuki"
internal const val SIGN_IN_PROMPT = "New to Yuki?"
internal const val SIGN_IN_CREATE_LABEL = "Create an account"

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
        ),
        modifier = modifier,
    )
}

data class SignInActions(
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onCreateAccountClick: () -> Unit,
)

@Composable
internal fun SignInScreen(
    state: SignInState,
    actions: SignInActions,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = SIGN_IN_TITLE, modifier = modifier) {
        AuthMessage(title = SIGN_IN_FAILED_TITLE, message = state.message)

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
            label = SIGN_IN_SUBMIT_LABEL,
            onClick = actions.onSubmit,
            modifier = Modifier.fillMaxWidth(),
            isEnabled = !state.isSubmitting,
        )

        AuthFooterPrompt(
            prompt = SIGN_IN_PROMPT,
            actionLabel = SIGN_IN_CREATE_LABEL,
            onActionClick = actions.onCreateAccountClick,
        )
    }
}
