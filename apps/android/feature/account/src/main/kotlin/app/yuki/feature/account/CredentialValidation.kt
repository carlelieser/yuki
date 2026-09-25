package app.yuki.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

const val MINIMUM_PASSWORD_LENGTH = 8

enum class CredentialError {
    EmailRequired,
    EmailInvalid,
    PasswordRequired,
    PasswordTooShort,
    NameRequired,
}

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")

private val errorText: Map<CredentialError, Int> = mapOf(
    CredentialError.EmailRequired to R.string.account_error_email_required,
    CredentialError.EmailInvalid to R.string.account_error_email_invalid,
    CredentialError.PasswordRequired to R.string.account_error_password_required,
    CredentialError.NameRequired to R.string.account_error_name_required,
)

internal fun emailError(email: String): CredentialError? {
    val trimmed = email.trim()

    return when {
        trimmed.isEmpty() -> CredentialError.EmailRequired
        !EMAIL_PATTERN.matches(trimmed) -> CredentialError.EmailInvalid
        else -> null
    }
}

internal fun passwordError(password: String): CredentialError? =
    CredentialError.PasswordRequired.takeIf { password.isEmpty() }

internal fun newPasswordError(password: String): CredentialError? =
    CredentialError.PasswordTooShort.takeIf { password.length < MINIMUM_PASSWORD_LENGTH }

internal fun nameError(name: String): CredentialError? =
    CredentialError.NameRequired.takeIf { name.isBlank() }

@Composable
internal fun CredentialError.text(): String = when (this) {
    CredentialError.PasswordTooShort -> pluralStringResource(
        R.plurals.account_error_password_too_short,
        MINIMUM_PASSWORD_LENGTH,
        MINIMUM_PASSWORD_LENGTH,
    )
    else -> stringResource(errorText.getValue(this))
}
