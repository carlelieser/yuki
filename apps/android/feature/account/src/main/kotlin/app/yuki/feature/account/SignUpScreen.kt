package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.component.rememberLinkOpener
import app.yuki.core.designsystem.theme.YukiSpacing

const val VERIFICATION_SENT_TAG = "verificationSent"

@Composable
fun SignUpRoute(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignUpScreen(
        state = state,
        baseUrl = viewModel.baseUrl,
        actions = SignUpActions(
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::onSubmit,
            onSignInClick = onSignInClick,
            onMessageShown = viewModel::onMessageShown,
        ),
        modifier = modifier,
    )
}

data class SignUpActions(
    val onNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onSignInClick: () -> Unit,
    val onMessageShown: () -> Unit,
)

@Composable
internal fun SignUpScreen(
    state: SignUpState,
    baseUrl: String,
    actions: SignUpActions,
    modifier: Modifier = Modifier,
) {
    val sentTo = state.verificationSentTo

    if (sentTo != null) {
        VerificationSent(email = sentTo, onSignInClick = actions.onSignInClick, modifier = modifier)
        return
    }

    SignUpForm(state = state, baseUrl = baseUrl, actions = actions, modifier = modifier)
}

@Composable
private fun SignUpForm(
    state: SignUpState,
    baseUrl: String,
    actions: SignUpActions,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = stringResource(R.string.account_sign_up_title),
        modifier = modifier,
        message = state.message?.text()?.let { text -> AuthMessage(text, actions.onMessageShown) },
    ) {
        SignUpFields(state = state, actions = actions)

        YukiButton(
            label = stringResource(R.string.account_sign_up_submit),
            onClick = actions.onSubmit,
            modifier = Modifier.fillMaxWidth(),
            isEnabled = !state.isSubmitting,
        )

        AuthFooterPrompt(
            prompt = stringResource(R.string.account_sign_up_prompt),
            actionLabel = stringResource(R.string.account_sign_up_sign_in),
            onActionClick = actions.onSignInClick,
        )

        LegalNotice(baseUrl = baseUrl, onLinkClick = rememberLinkOpener())
    }
}

@Composable
private fun SignUpFields(state: SignUpState, actions: SignUpActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
    ) {
        NameField(
            field = CredentialField(
                value = state.name,
                error = state.nameError,
                onValueChange = actions.onNameChange,
            ),
        )
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
}

@Composable
private fun VerificationSent(
    email: String,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = stringResource(R.string.account_verification_title),
        modifier = modifier.testTag(VERIFICATION_SENT_TAG),
    ) {
        Text(
            text = stringResource(R.string.account_verification_body, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        YukiButton(
            label = stringResource(R.string.account_verification_back),
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
