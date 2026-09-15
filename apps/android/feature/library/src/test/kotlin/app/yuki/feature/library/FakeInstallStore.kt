package app.yuki.feature.library

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeInstallStore(initial: List<InstalledApp> = emptyList()) : InstallStore {
    private val rows = MutableStateFlow(initial)

    val forgottenPackages = mutableListOf<List<String>>()

    var forgetCalls = 0
        private set

    override fun observeInstalls(): Flow<List<InstalledApp>> = rows

    override suspend fun installs(): List<InstalledApp> = rows.value

    override suspend fun packageNameOf(githubRepoId: Long): String? =
        rows.value.firstOrNull { app -> app.githubRepoId == githubRepoId }?.packageName

    override suspend fun record(recording: InstallRecording) {
        rows.value = rows.value.filterNot { app ->
            app.githubRepoId == recording.app.githubRepoId
        } + recording.app
    }

    override suspend fun forget(githubRepoId: Long) {
        rows.value = rows.value.filterNot { app -> app.githubRepoId == githubRepoId }
    }

    override suspend fun forgetPackages(packageNames: List<String>) {
        forgetCalls += 1
        forgottenPackages += packageNames
        rows.value = rows.value.filterNot { app -> app.packageName in packageNames }
    }
}

internal class FakeInstalledPackages(
    private val present: MutableSet<String> = mutableSetOf(),
) : InstalledPackages {
    private val launchable = mutableSetOf<String>()

    override fun isPresent(packageName: String): Boolean = packageName in present

    override fun launchIntentExists(packageName: String): Boolean = packageName in launchable

    fun install(packageName: String, isLaunchable: Boolean = true) {
        present += packageName
        if (isLaunchable) launchable += packageName
    }

    fun uninstall(packageName: String) {
        present -= packageName
        launchable -= packageName
    }
}

internal class FakeLibraryProgressStore : InstallProgressStore {
    private val rows = MutableStateFlow<List<InstallProgress>>(emptyList())

    var settledClearances = 0
        private set

    override fun observe(githubRepoId: Long): Flow<InstallProgress?> =
        rows.map { current -> current.firstOrNull { it.githubRepoId == githubRepoId } }

    override fun observeActive(): Flow<List<InstallProgress>> = rows

    override suspend fun find(githubRepoId: Long): InstallProgress? =
        rows.value.firstOrNull { it.githubRepoId == githubRepoId }

    override suspend fun unsettled(): List<InstallProgress> = rows.value.filter { row ->
        row.state is InstallState.Downloading ||
            row.state == InstallState.Installing ||
            row.state == InstallState.PendingUserAction
    }

    override suspend fun write(progress: InstallProgress) {
        val existing = rows.value.indexOfFirst { it.githubRepoId == progress.githubRepoId }

        rows.value = if (existing < 0) {
            rows.value + progress
        } else {
            rows.value.toMutableList().apply { set(existing, progress) }
        }
    }

    override suspend fun clear(githubRepoId: Long) {
        rows.value = rows.value.filterNot { it.githubRepoId == githubRepoId }
    }

    override suspend fun clearSettled() {
        settledClearances += 1
        rows.value = rows.value.filterNot { row -> row.state is InstallState.Installed }
    }

}
