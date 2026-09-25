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
)

@HiltViewModel
class SignInViewModel @Inject internal constructor(
    private val repository: AuthRepository,
    private val store: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInState())

    val state: StateFlow<SignInState> = mutableState.asStateFlow()

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
    is FailureReason.Rejected -> AccountMessage.Explanation(reason.explanation)
    is FailureReason.Server -> AccountMessage.Unavailable
    is FailureReason.Unexpected -> AccountMessage.Unavailable
}
