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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.component.rememberLinkOpener
import app.yuki.core.designsystem.theme.YukiSpacing

const val SIGN_UP_SUBMIT_LABEL = "Create account"
const val VERIFICATION_SENT_TAG = "verificationSent"

internal const val SIGN_UP_TITLE = "Sign up for Yuki"
internal const val SIGN_UP_PROMPT = "Already have an account?"
internal const val SIGN_UP_SIGN_IN_LABEL = "Sign in"
internal const val VERIFICATION_TITLE = "Check your email"
internal const val VERIFICATION_BACK_LABEL = "Back to sign in"

private fun verificationBody(email: String) =
    "We sent a verification link to $email. Open it to finish setting up your account."

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
        title = SIGN_UP_TITLE,
        modifier = modifier,
        message = state.message?.let { text -> AuthMessage(text, actions.onMessageShown) },
    ) {
        SignUpFields(state = state, actions = actions)

        YukiButton(
            label = SIGN_UP_SUBMIT_LABEL,
            onClick = actions.onSubmit,
            modifier = Modifier.fillMaxWidth(),
            isEnabled = !state.isSubmitting,
        )

        AuthFooterPrompt(
            prompt = SIGN_UP_PROMPT,
            actionLabel = SIGN_UP_SIGN_IN_LABEL,
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
    AuthScaffold(title = VERIFICATION_TITLE, modifier = modifier.testTag(VERIFICATION_SENT_TAG)) {
        Text(
            text = verificationBody(email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        YukiButton(
            label = VERIFICATION_BACK_LABEL,
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
