package app.yuki.feature.updates

import app.yuki.core.installer.InstallCoordinator
import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallSource
import app.yuki.core.installer.InstallTarget
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
    fun install(update: AvailableUpdate): Flow<InstallState>
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
internal class CoordinatorUpdateInstaller @Inject constructor(
    private val coordinator: InstallCoordinator,
    @param:YukiBaseUrl private val baseUrl: String,
) : UpdateInstaller {
    override fun install(update: AvailableUpdate): Flow<InstallState> =
        coordinator.install(update.toRequest(baseUrl))
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateInstallerModule {
    @Binds
    abstract fun bindInstaller(installer: CoordinatorUpdateInstaller): UpdateInstaller
}
