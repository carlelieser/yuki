package app.yuki.feature.updates

import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallScheduler
import app.yuki.core.installer.InstallSource
import app.yuki.core.installer.InstallTarget
import app.yuki.core.installer.observeActiveStates
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.InstallState
import app.yuki.core.model.deviceArchitecture
import app.yuki.core.model.downloadUrl
import app.yuki.core.network.YukiBaseUrl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface UpdateInstaller {
    fun install(update: AvailableUpdate)

    fun observeActiveStates(): Flow<Map<Long, InstallState>>

    suspend fun cancel(githubRepoId: Long)
}

internal fun AvailableUpdate.toInstallState(): InstallState =
    InstallState.UpdateAvailable(from = installed.versionTag, to = version.tag)

private fun AvailableUpdate.toRequest(baseUrl: String): InstallRequest = InstallRequest(
    target = InstallTarget(
        githubRepoId = installed.githubRepoId,
        slug = installed.slug,
        title = installed.title,
        iconUrl = installed.iconUrl,
    ),
    source = InstallSource(
        downloadUrl = downloadUrl(
            baseUrl = baseUrl,
            slug = installed.slug,
            versionTag = version.tag,
            architecture = deviceArchitecture(),
        ),
        versionTag = version.tag,
        assetName = version.assetName,
    ),
)

@Singleton
internal class SchedulerUpdateInstaller @Inject constructor(
    private val scheduler: InstallScheduler,
    private val progress: InstallProgressStore,
    @param:YukiBaseUrl private val baseUrl: String,
) : UpdateInstaller {
    override fun install(update: AvailableUpdate) = scheduler.start(update.toRequest(baseUrl))

    override fun observeActiveStates(): Flow<Map<Long, InstallState>> =
        progress.observeActiveStates()

    override suspend fun cancel(githubRepoId: Long) = scheduler.cancel(githubRepoId)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateInstallerModule {
    @Binds
    abstract fun bindInstaller(installer: SchedulerUpdateInstaller): UpdateInstaller
}
