package app.yuki.feature.library

import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import javax.inject.Inject

internal class LibraryReconciler @Inject constructor(
    private val store: InstallStore,
    private val packages: InstalledPackages,
) {
    suspend fun reconcile(installs: List<InstalledApp>): List<InstalledApp> {
        val (present, uninstalled) = installs.partition { app -> packages.isPresent(app.packageName) }
        if (uninstalled.isEmpty()) return present

        store.forgetPackages(uninstalled.map(InstalledApp::packageName))

        return present
    }
}
