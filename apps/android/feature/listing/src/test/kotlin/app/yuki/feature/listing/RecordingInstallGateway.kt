package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

internal class RecordingInstallGateway : ListingInstallGateway {
    private val observed = MutableStateFlow<ListingInstallStatus?>(null)

    val requests: MutableList<ListingInstallRequest> = mutableListOf()
    val cancelled: MutableList<Long> = mutableListOf()
    val opened: MutableList<Long> = mutableListOf()
    val uninstalled: MutableList<Long> = mutableListOf()

    var isSilent: Boolean = false

    fun emitObserved(next: InstallState, versionTag: String? = null) {
        observed.value = ListingInstallStatus(state = next, versionTag = versionTag)
    }

    override suspend fun install(request: ListingInstallRequest) {
        requests.add(request)
    }

    override fun observe(githubRepoId: Long): Flow<ListingInstallStatus> =
        observed.filterNotNull()

    override suspend fun cancel(githubRepoId: Long) {
        cancelled.add(githubRepoId)
    }

    override suspend fun open(githubRepoId: Long) {
        opened.add(githubRepoId)
    }

    var uninstallError: Throwable? = null

    override suspend fun uninstall(githubRepoId: Long) {
        uninstalled.add(githubRepoId)
        uninstallError?.let { error -> throw error }
    }

    override suspend fun isSilentUninstall(): Boolean = isSilent
}
