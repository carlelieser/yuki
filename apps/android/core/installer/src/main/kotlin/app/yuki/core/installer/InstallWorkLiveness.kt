package app.yuki.core.installer

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal interface InstallWorkLiveness {
    suspend fun isLive(githubRepoId: Long): Boolean
}

internal class WorkManagerInstallWorkLiveness @Inject constructor(
    private val workManager: WorkManager,
) : InstallWorkLiveness {
    override suspend fun isLive(githubRepoId: Long): Boolean =
        workManager
            .getWorkInfosForUniqueWork(installWorkName(githubRepoId))
            .awaitResult()
            .hasLiveWork()
}

internal fun List<WorkInfo>.hasLiveWork(): Boolean = any { info -> !info.state.isFinished }

private suspend fun <T> ListenableFuture<T>.awaitResult(): T = suspendCancellableCoroutine {
        continuation ->
    addListener(
        {
            runCatching { get() }
                .onSuccess { value -> if (continuation.isActive) continuation.resume(value) }
                .onFailure { error -> continuation.resumeWith(Result.failure(error)) }
        },
        Executor(Runnable::run),
    )

    continuation.invokeOnCancellation { cancel(false) }
}
