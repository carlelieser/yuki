package app.yuki.core.model

fun interface NetworkStatus {
    fun isOnline(): Boolean
}

fun NetworkStatus.connectionFailure(): FailureReason =
    if (isOnline()) FailureReason.Unreachable else FailureReason.Offline
