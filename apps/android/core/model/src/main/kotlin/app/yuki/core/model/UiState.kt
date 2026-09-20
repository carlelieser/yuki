package app.yuki.core.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(val data: T) : UiState<T>

    data class Failure(val reason: FailureReason) : UiState<Nothing>
}

sealed interface FailureReason {
    data object Offline : FailureReason

    data object NotFound : FailureReason

    data object Unauthorized : FailureReason

    data object EmailNotVerified : FailureReason

    data object AccountExists : FailureReason

    data class Rejected(val explanation: String) : FailureReason

    data class Server(val status: Int) : FailureReason

    data class Unexpected(val cause: Throwable) : FailureReason
}
