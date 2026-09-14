package app.yuki.feature.updates

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.SelfListing
import app.yuki.core.model.resolveInstalledTag
import app.yuki.core.network.ListingRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelfInstallReconciler @Inject internal constructor(
    private val store: InstallStore,
    private val repository: ListingRepository,
    private val self: SelfListing,
    private val clock: Clock,
) {
    suspend fun reconcile(): Result<String?> {
        val detail = repository.detail(self.slug).getOrElse { error ->
            return Result.failure(error)
        }

        val tag = resolveInstalledTag(self.versionName, detail.versions)
            ?: return Result.success(null)

        store.record(
            InstallRecording(
                app = InstalledApp(
                    githubRepoId = self.githubRepoId,
                    packageName = self.packageName,
                    slug = self.slug,
                    title = detail.title,
                    iconUrl = detail.summary.iconUrl,
                    versionTag = tag,
                ),
                versionCode = self.versionCode,
                installedAt = clock.instant(),
            ),
        )

        return Result.success(tag)
    }
}
