package app.yuki.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.FailureReason
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.YukiBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: CredentialError? = null,
    val emailError: CredentialError? = null,
    val passwordError: CredentialError? = null,
    val message: AccountMessage? = null,
    val isSubmitting: Boolean = false,
    val verificationSentTo: String? = null,
)

@HiltViewModel
class SignUpViewModel @Inject internal constructor(
    private val repository: AuthRepository,
    @param:YukiBaseUrl val baseUrl: String,
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

    fun onMessageShown() {
        mutableState.update { state -> state.copy(message = null) }
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

private fun messageFor(reason: FailureReason): AccountMessage = when (reason) {
    FailureReason.AccountExists -> AccountMessage.EmailTaken
    FailureReason.Offline -> AccountMessage.Offline
    else -> AccountMessage.Unavailable
}
