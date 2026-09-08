package app.yuki.core.shizuku

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

class ShizukuLifecycleObserver @Inject constructor(
    private val monitor: ShizukuMonitor,
) : DefaultLifecycleObserver {
    fun bindTo(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        monitor.start()
    }

    override fun onResume(owner: LifecycleOwner) {
        monitor.refresh()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        monitor.stop()
        owner.lifecycle.removeObserver(this)
    }
}
