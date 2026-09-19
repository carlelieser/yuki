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

internal const val SIGN_IN_INVALID = "Invalid email or password."
internal const val SIGN_IN_UNVERIFIED = "Verify your email address before signing in."
internal const val SIGN_IN_RESEND_LABEL = "Resend"
internal const val SIGN_IN_RESENT = "We sent a new link to your email address."
internal const val SIGN_IN_RESEND_FAILED = "We could not send a new link. Try again in a moment."
internal const val SIGN_IN_OFFLINE = "You're offline. Check your connection and try again."
internal const val SIGN_IN_UNAVAILABLE = "Yuki is not responding right now. Try again in a moment."

data class SignInState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val message: String? = null,
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
            val message = if (outcome.isSuccess) SIGN_IN_RESENT else SIGN_IN_RESEND_FAILED

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

private fun messageFor(reason: FailureReason): String = when (reason) {
    FailureReason.Unauthorized -> SIGN_IN_INVALID
    FailureReason.EmailNotVerified -> SIGN_IN_UNVERIFIED
    FailureReason.Offline -> SIGN_IN_OFFLINE
    FailureReason.NotFound -> SIGN_IN_INVALID
    FailureReason.AccountExists -> SIGN_IN_INVALID
    is FailureReason.Server -> SIGN_IN_UNAVAILABLE
    is FailureReason.Unexpected -> SIGN_IN_UNAVAILABLE
}
