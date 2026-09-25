package app.yuki.install

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallScheduler
import app.yuki.core.installer.InstallSource
import app.yuki.core.model.InstallState
import app.yuki.core.model.deviceArchitecture
import app.yuki.core.model.downloadUrl
import app.yuki.core.network.YukiBaseUrl
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun interface InstallRestarter {
    suspend fun restart(request: InstallRequest)
}

@Singleton
internal class InstallFailureNotices(
    private val progress: InstallProgressStore,
    private val restarter: InstallRestarter,
    private val baseUrl: String,
) {
    @Inject
    constructor(
        progress: InstallProgressStore,
        scheduler: InstallScheduler,
        @YukiBaseUrl baseUrl: String,
    ) : this(progress, InstallRestarter(scheduler::start), baseUrl)

    fun oldestFailure(): Flow<InstallProgress?> = progress.observeActive()
        .map { rows -> rows.firstOrNull { row -> row.state is InstallState.Failed } }
        .distinctUntilChanged()

    suspend fun dismiss(failed: InstallProgress) = progress.clear(failed.githubRepoId)

    suspend fun retry(failed: InstallProgress) {
        progress.clear(failed.githubRepoId)
        restarter.restart(failed.toRetryRequest(baseUrl))
    }
}

internal fun InstallProgress.toRetryRequest(baseUrl: String): InstallRequest = InstallRequest(
    target = target,
    source = InstallSource(
        downloadUrl = downloadUrl(
            baseUrl = baseUrl,
            slug = target.slug,
            versionTag = versionTag,
            architecture = deviceArchitecture(),
        ),
        versionTag = versionTag,
        assetName = null,
    ),
)
