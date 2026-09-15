package app.yuki.feature.library

import javax.inject.Inject

internal class LibraryDependencies @Inject constructor(
    val reconciler: LibraryReconciler,
    val detection: PackageDetectionReconciler,
    val packages: InstalledPackages,
)
