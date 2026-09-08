package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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

internal object UnwiredInstallGateway : ListingInstallGateway {
    override fun install(request: ListingInstallRequest): Flow<InstallState> = emptyFlow()

    override fun observe(githubRepoId: Long): Flow<InstallState> = emptyFlow()

    override suspend fun cancel(githubRepoId: Long) = Unit

    override suspend fun open(githubRepoId: Long) = Unit
}

@Module
@InstallIn(SingletonComponent::class)
internal object ListingInstallModule {
    @Provides
    @Singleton
    fun provideInstallGateway(): ListingInstallGateway = UnwiredInstallGateway
}
