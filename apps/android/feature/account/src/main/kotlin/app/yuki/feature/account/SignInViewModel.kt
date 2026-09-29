package app.yuki.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.FailureReason
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.SignedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInState(
    val email: String = "",
    val password: String = "",
    val emailError: CredentialError? = null,
    val passwordError: CredentialError? = null,
    val message: AccountMessage? = null,
    val canResendVerification: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSignedIn: Boolean = false,
    val browserUrl: String? = null,
)

@HiltViewModel
class SignInViewModel @Inject internal constructor(
    private val repository: AuthRepository,
    private val store: SessionStore,
    private val githubSignIn: GithubSignIn,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInState())

    private var githubStart: Job? = null

    val state: StateFlow<SignInState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            githubSignIn.outcomes().collect { outcome -> mutableState.update(outcome::applyTo) }
        }
    }

    fun onGithubClick() {
        val current = mutableState.value
        if (current.isSubmitting || current.browserUrl != null) return
        if (githubStart?.isActive == true) return

        githubStart = viewModelScope.launch {
            val url = githubSignIn.start()
            mutableState.update { state -> state.copy(browserUrl = url, message = null) }
        }
    }

    fun onBrowserLaunched() {
        mutableState.update { state -> state.copy(browserUrl = null) }
    }

    fun onEmailChange(email: String) {
        mutableState.update { state -> state.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        mutableState.update { state -> state.copy(password = password, passwordError = null) }
    }

    fun onSubmit() {
        val current = mutableState.value
        if (current.isSubmitting) return

        val validated = current.validated()
        mutableState.value = validated

        if (validated.emailError != null || validated.passwordError != null) return

        submit(validated.email.trim(), validated.password)
    }

    private fun submit(email: String, password: String) {
        mutableState.update { state ->
            state.copy(isSubmitting = true, message = null, canResendVerification = false)
        }

        viewModelScope.launch {
            repository.signIn(email, password)
                .onSuccess { signedIn -> store(signedIn) }
                .onFailure { error -> report(error.failureReason()) }
        }
    }

    private suspend fun store(signedIn: SignedIn) {
        val token = signedIn.token

        if (token == null) {
            report(FailureReason.EmailNotVerified)
            return
        }

        store.store(AuthSession(token = token, account = signedIn.account))
        mutableState.update { state -> state.copy(isSubmitting = false, isSignedIn = true) }
    }

    fun onMessageShown() {
        mutableState.update { state -> state.copy(message = null, canResendVerification = false) }
    }

    fun onResendVerification() {
        val email = mutableState.value.email.trim()
        if (email.isEmpty()) return

        viewModelScope.launch {
            val outcome = repository.sendVerificationEmail(email)
            val message = if (outcome.isSuccess) {
                AccountMessage.VerificationResent
            } else {
                AccountMessage.VerificationResendFailed
            }

            mutableState.update { state ->
                state.copy(message = message, canResendVerification = false)
            }
        }
    }

    private fun report(reason: FailureReason) {
        mutableState.update { state ->
            state.copy(
                isSubmitting = false,
                message = messageFor(reason),
                canResendVerification = reason == FailureReason.EmailNotVerified,
            )
        }
    }
}

private fun GithubSignInOutcome.applyTo(state: SignInState): SignInState = when (this) {
    GithubSignInOutcome.SignedIn -> state.copy(isSignedIn = true)
    is GithubSignInOutcome.Failed -> state.copy(message = message, canResendVerification = false)
}

private fun SignInState.validated(): SignInState = copy(
    emailError = emailError(email),
    passwordError = passwordError(password),
)

private fun messageFor(reason: FailureReason): AccountMessage = when (reason) {
    FailureReason.Unauthorized -> AccountMessage.InvalidCredentials
    FailureReason.EmailNotVerified -> AccountMessage.EmailUnverified
    FailureReason.Offline -> AccountMessage.Offline
    FailureReason.NotFound -> AccountMessage.InvalidCredentials
    FailureReason.AccountExists -> AccountMessage.InvalidCredentials
    FailureReason.LastSignInMethod -> AccountMessage.Unavailable
    FailureReason.ReauthenticationRequired -> AccountMessage.Unavailable
    is FailureReason.Rejected -> AccountMessage.Explanation(reason.explanation)
    is FailureReason.Server -> AccountMessage.Unavailable
    is FailureReason.Unexpected -> AccountMessage.Unavailable
}
