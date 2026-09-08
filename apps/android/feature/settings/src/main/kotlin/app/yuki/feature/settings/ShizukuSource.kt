package app.yuki.feature.settings

import app.yuki.core.shizuku.ShizukuDetail
import app.yuki.core.shizuku.ShizukuMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

interface ShizukuSource {
    val detail: StateFlow<ShizukuDetail>

    fun refresh()

    fun requestPermission()
}

@Singleton
internal class MonitorShizukuSource @Inject constructor(
    private val monitor: ShizukuMonitor,
) : ShizukuSource {
    override val detail: StateFlow<ShizukuDetail> = monitor.detail

    override fun refresh() {
        monitor.refresh()
    }

    override fun requestPermission() {
        monitor.requestPermission()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShizukuSourceModule {
    @Binds
    @Singleton
    abstract fun bindSource(source: MonitorShizukuSource): ShizukuSource
}
