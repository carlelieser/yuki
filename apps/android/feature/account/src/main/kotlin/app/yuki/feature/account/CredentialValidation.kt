package app.yuki.feature.account

const val MINIMUM_PASSWORD_LENGTH = 8

internal const val EMAIL_REQUIRED = "Email is required."
internal const val EMAIL_INVALID = "Enter a valid email address."
internal const val PASSWORD_REQUIRED = "Password is required."
internal const val PASSWORD_TOO_SHORT =
    "Password must be at least $MINIMUM_PASSWORD_LENGTH characters."
internal const val NAME_REQUIRED = "Name is required."

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")

internal fun emailError(email: String): String? {
    val trimmed = email.trim()

    return when {
        trimmed.isEmpty() -> EMAIL_REQUIRED
        !EMAIL_PATTERN.matches(trimmed) -> EMAIL_INVALID
        else -> null
    }
}

internal fun passwordError(password: String): String? =
    PASSWORD_REQUIRED.takeIf { password.isEmpty() }

internal fun newPasswordError(password: String): String? =
    PASSWORD_TOO_SHORT.takeIf { password.length < MINIMUM_PASSWORD_LENGTH }

internal fun nameError(name: String): String? = NAME_REQUIRED.takeIf { name.isBlank() }
