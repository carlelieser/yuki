package app.yuki.core.model

interface FailureAware {
    val reason: FailureReason
}

fun Throwable.failureReason(): FailureReason = when (this) {
    is FailureAware -> reason
    else -> FailureReason.Unexpected(this)
}

fun <T> Result<T>.toUiState(): UiState<T> = fold(
    onSuccess = { value -> UiState.Success(value) },
    onFailure = { error -> UiState.Failure(error.failureReason()) },
)
