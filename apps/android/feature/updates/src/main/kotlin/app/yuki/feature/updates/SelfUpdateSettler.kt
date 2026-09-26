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
        if (!self.isRelease) return false

        val pending = progress.find(self.githubRepoId) ?: return false
        if (pending.versionTag != self.releaseTag) return false

        installer.cancel(self.githubRepoId)
        return true
    }
}
