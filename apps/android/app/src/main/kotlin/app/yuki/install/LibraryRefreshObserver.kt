package app.yuki.install

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.yuki.feature.library.DetectedInstallRefresh
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "LibraryRefresh"

internal fun interface RefreshFailureLogger {
    fun onFailure(message: String, error: Throwable)
}

@Singleton
internal class LibraryRefreshObserver(
    private val detection: DetectedInstallRefresh,
    private val scope: CoroutineScope,
    private val logFailure: RefreshFailureLogger,
) : DefaultLifecycleObserver {
    @Inject
    constructor(detection: DetectedInstallRefresh) : this(
        detection = detection,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        logFailure = { message, error -> android.util.Log.w(TAG, message, error) },
    )

    private val running = AtomicBoolean(false)

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        refresh()
    }

    fun refresh() {
        if (!running.compareAndSet(false, true)) return

        scope.launch {
            try {
                runCatching { detection.reconcile() }.onFailure { error ->
                    logFailure.onFailure("Could not reconcile detected installs", error)
                }
            } finally {
                running.set(false)
            }
        }
    }
}
