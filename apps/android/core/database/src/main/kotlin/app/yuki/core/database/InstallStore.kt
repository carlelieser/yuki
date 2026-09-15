package app.yuki.core.database

import app.yuki.core.model.InstalledApp
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class InstallRecording(
    val app: InstalledApp,
    val versionCode: Long?,
    val installedAt: Instant,
)

interface InstallStore {
    fun observeInstalls(): Flow<List<InstalledApp>>

    suspend fun installs(): List<InstalledApp>

    suspend fun packageNameOf(githubRepoId: Long): String?

    suspend fun record(recording: InstallRecording)

    suspend fun forget(githubRepoId: Long)

    suspend fun forgetPackages(packageNames: List<String>)
}

@Singleton
internal class RoomInstallStore @Inject constructor(
    private val dao: InstallDao,
) : InstallStore {
    override fun observeInstalls(): Flow<List<InstalledApp>> =
        dao.observeAll().map { entities -> entities.map(InstallEntity::toInstalledApp) }

    override suspend fun installs(): List<InstalledApp> =
        dao.getAll().map(InstallEntity::toInstalledApp)

    override suspend fun packageNameOf(githubRepoId: Long): String? =
        dao.findByRepoId(githubRepoId)?.packageName

    override suspend fun record(recording: InstallRecording) = dao.upsert(recording.toEntity())

    override suspend fun forget(githubRepoId: Long) = dao.deleteByRepoId(githubRepoId)

    override suspend fun forgetPackages(packageNames: List<String>) {
        if (packageNames.isEmpty()) return

        dao.deleteByPackageNames(packageNames)
    }
}

private fun InstallRecording.toEntity(): InstallEntity = InstallEntity(
    githubRepoId = app.githubRepoId,
    packageName = app.packageName,
    slug = app.slug,
    title = app.title,
    iconUrl = app.iconUrl,
    versionTag = app.versionTag,
    versionCode = versionCode,
    installedAt = installedAt,
    source = app.source.name,
)
