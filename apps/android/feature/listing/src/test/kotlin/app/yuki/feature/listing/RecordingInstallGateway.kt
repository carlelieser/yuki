package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

internal class RecordingInstallGateway : ListingInstallGateway {
    private val observed = MutableStateFlow<ListingInstallStatus?>(null)
    private val installedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val activeStates = MutableStateFlow<Map<Long, InstallState>>(emptyMap())

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

    override fun observeInstalledIds(): Flow<Set<Long>> = installedIds

    override fun observeActiveStates(): Flow<Map<Long, InstallState>> = activeStates

    fun markInstalled(githubRepoId: Long) {
        installedIds.value = installedIds.value + githubRepoId
    }

    fun markActive(githubRepoId: Long, state: InstallState) {
        activeStates.value = activeStates.value + (githubRepoId to state)
    }

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
