package app.yuki

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.yuki.core.installer.StuckInstallReclaimer
import app.yuki.feature.updates.SelfInstallReconciler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "YukiApplication"

@HiltAndroidApp
class YukiApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var selfInstalls: SelfInstallReconciler

    @Inject
    lateinit var stuckInstalls: StuckInstallReclaimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            stuckInstalls.reclaim().onFailure { error ->
                Log.w(TAG, "Could not reclaim installs stranded by an earlier run", error)
            }
        }

        scope.launch {
            selfInstalls.reconcile().onFailure { error ->
                Log.w(TAG, "Could not reconcile Yuki's own install record", error)
            }
        }
    }
}
