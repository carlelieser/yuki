package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import kotlinx.coroutines.flow.Flow

data class ListingInstallRequest(
    val detail: ListingDetail,
    val version: InstallableVersion,
) {
    val downloadUrl: String get() = version.downloadUrl
}

data class ListingInstallStatus(
    val state: InstallState,
    val versionTag: String?,
)

interface ListingInstallGateway {
    suspend fun install(request: ListingInstallRequest)

    fun observe(githubRepoId: Long): Flow<ListingInstallStatus>

    fun observeInstalledIds(): Flow<Set<Long>>

    fun observeActiveStates(): Flow<Map<Long, InstallState>>

    suspend fun cancel(githubRepoId: Long)

    suspend fun open(githubRepoId: Long)

    suspend fun uninstall(githubRepoId: Long)

    suspend fun isSilentUninstall(): Boolean
}
