package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion
import kotlinx.coroutines.flow.Flow

data class ListingInstallRequest(
    val detail: ListingDetail,
    val version: ListingVersion,
) {
    val downloadUrl: String
        get() = requireNotNull(version.downloadUrl) {
            "Version ${version.tag} of ${detail.slug} has no download url"
        }
}

interface ListingInstallGateway {
    fun install(request: ListingInstallRequest): Flow<InstallState>

    fun observe(githubRepoId: Long): Flow<InstallState>

    suspend fun cancel(githubRepoId: Long)

    suspend fun open(githubRepoId: Long)
}
