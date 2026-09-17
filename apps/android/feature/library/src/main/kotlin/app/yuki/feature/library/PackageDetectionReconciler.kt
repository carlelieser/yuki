package app.yuki.feature.library

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.database.PackageIndexStore
import app.yuki.core.model.CatalogPackage
import app.yuki.core.network.ListingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageDetectionReconciler @Inject internal constructor(
    private val store: InstallStore,
    private val index: PackageIndexStore,
    private val repository: ListingRepository,
    private val packages: InstalledPackages,
) : DetectedInstallRefresh {
    override suspend fun reconcile() {
        val catalog = refreshIndex()
        if (catalog.isEmpty()) return

        val device = packages.findAll(catalog.map(CatalogPackage::packageName))
        val plan = planDetection(device, catalog, store.installs())

        plan.detected.forEach { detected ->
            store.record(
                InstallRecording(
                    app = detected.app,
                    versionCode = detected.versionCode,
                    installedAt = detected.installedAt,
                ),
            )
        }

        plan.forgotten.forEach { githubRepoId -> store.forget(githubRepoId) }
    }

    private suspend fun refreshIndex(): List<CatalogPackage> {
        val downloaded = repository.packages().getOrNull()
        if (downloaded != null) {
            index.replaceAll(downloaded)
            return downloaded
        }

        return index.packages()
    }
}
