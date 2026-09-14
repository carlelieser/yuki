package app.yuki.core.shizuku

import app.yuki.core.installer.InstallException
import app.yuki.core.model.InstallFailure
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

internal val USER_SERVICE_BIND_TIMEOUT: Duration = 40.seconds

internal val PRIVILEGED_INSTALL_TIMEOUT: Duration = 10.minutes

internal suspend fun <T> withInstallTimeout(
    timeout: Duration,
    message: String,
    block: suspend () -> T,
): T = try {
    withTimeout(timeout) { block() }
} catch (error: TimeoutCancellationException) {
    throw InstallException(InstallFailure.TimedOut, message, error)
}
