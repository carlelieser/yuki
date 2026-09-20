package app.yuki.feature.library

import javax.inject.Inject

internal class LibrarySync @Inject constructor(
    private val detection: DetectedInstallRefresh,
) {
    suspend fun run() {
        detection.reconcile()
    }
}
