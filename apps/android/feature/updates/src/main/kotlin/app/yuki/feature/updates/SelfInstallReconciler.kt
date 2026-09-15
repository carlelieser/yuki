package app.yuki.feature.updates

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.SelfListing
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfInstallReconciler @Inject internal constructor(
    private val store: InstallStore,
    private val self: SelfListing,
    private val clock: Clock,
) {
    suspend fun reconcile(): String? {
        if (!self.isRelease) return null

        store.record(
            InstallRecording(
                app = InstalledApp(
                    githubRepoId = self.githubRepoId,
                    packageName = self.packageName,
                    slug = self.slug,
                    title = self.title,
                    iconUrl = self.iconUrl,
                    versionTag = self.releaseTag,
                ),
                versionCode = self.versionCode,
                installedAt = clock.instant(),
            ),
        )

        return self.releaseTag
    }
}
