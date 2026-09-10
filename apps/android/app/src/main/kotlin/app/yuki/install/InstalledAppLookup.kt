package app.yuki.install

import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.network.YukiBaseUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InstalledAppLookup @Inject constructor(
    private val store: InstallStore,
    private val presence: InstalledPackagePresence,
    @param:YukiBaseUrl val baseUrl: String,
) {
    suspend fun find(githubRepoId: Long): InstalledApp? =
        store.installs().firstOrNull { app -> app.githubRepoId == githubRepoId }

    suspend fun packageNameOf(githubRepoId: Long): String? = store.packageNameOf(githubRepoId)

    fun isPresent(packageName: String): Boolean = presence.isPresent(packageName)
}
