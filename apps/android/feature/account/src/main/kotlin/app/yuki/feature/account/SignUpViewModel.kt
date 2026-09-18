package app.yuki.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.FailureReason
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val SIGN_UP_FAILED_TITLE = "Could not create your account"
internal const val SIGN_UP_TAKEN = "An account with that email already exists."
internal const val SIGN_UP_OFFLINE = "You're offline. Check your connection and try again."
internal const val SIGN_UP_UNAVAILABLE = "Yuki is not responding right now. Try again in a moment."

data class SignUpState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val message: String? = null,
    val isSubmitting: Boolean = false,
    val verificationSentTo: String? = null,
)

@HiltViewModel
class SignUpViewModel @Inject internal constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignUpState())

    val state: StateFlow<SignUpState> = mutableState.asStateFlow()

    fun onNameChange(name: String) {
        mutableState.update { state -> state.copy(name = name, nameError = null) }
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

        if (validated.hasErrors) return

        submit(validated.name.trim(), validated.email.trim(), validated.password)
    }

    private fun submit(name: String, email: String, password: String) {
        mutableState.update { state -> state.copy(isSubmitting = true, message = null) }

        viewModelScope.launch {
            repository.signUp(name, email, password)
                .onSuccess { accepted(email) }
                .onFailure { error -> report(error.failureReason()) }
        }
    }

    private fun accepted(email: String) {
        mutableState.update { state ->
            state.copy(isSubmitting = false, verificationSentTo = email)
        }
    }

    private fun report(reason: FailureReason) {
        mutableState.update { state ->
            state.copy(isSubmitting = false, message = messageFor(reason))
        }
    }
}

private val SignUpState.hasErrors: Boolean
    get() = nameError != null || emailError != null || passwordError != null

private fun SignUpState.validated(): SignUpState = copy(
    nameError = nameError(name),
    emailError = emailError(email),
    passwordError = newPasswordError(password),
)

private fun messageFor(reason: FailureReason): String = when (reason) {
    FailureReason.AccountExists -> SIGN_UP_TAKEN
    FailureReason.Offline -> SIGN_UP_OFFLINE
    else -> SIGN_UP_UNAVAILABLE
}
