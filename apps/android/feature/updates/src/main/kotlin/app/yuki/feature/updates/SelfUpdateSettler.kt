package app.yuki.feature.updates

import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.SelfListing
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfUpdateSettler @Inject internal constructor(
    private val self: SelfListing,
    private val progress: InstallProgressStore,
    private val installer: UpdateInstaller,
) {
    suspend fun settle(): Boolean {
        val pending = progress.find(self.githubRepoId) ?: return false
        if (!self.hasLanded(pending.versionTag)) return false

        installer.cancel(self.githubRepoId)
        return true
    }
}
