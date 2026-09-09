package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull

internal class RecordingInstallGateway : ListingInstallGateway {
    private val observed = MutableStateFlow<InstallState?>(null)
    private val installProgress = MutableSharedFlow<InstallState>(replay = 8)

    val requests: MutableList<ListingInstallRequest> = mutableListOf()
    val cancelled: MutableList<Long> = mutableListOf()
    val opened: MutableList<Long> = mutableListOf()

    fun emitObserved(next: InstallState) {
        observed.value = next
    }

    suspend fun emitInstallProgress(next: InstallState) {
        installProgress.emit(next)
    }

    override fun install(request: ListingInstallRequest): Flow<InstallState> {
        requests.add(request)
        return installProgress.asSharedFlow()
    }

    override fun observe(githubRepoId: Long): Flow<InstallState> = observed.filterNotNull()

    override suspend fun cancel(githubRepoId: Long) {
        cancelled.add(githubRepoId)
    }

    override suspend fun open(githubRepoId: Long) {
        opened.add(githubRepoId)
    }
}
