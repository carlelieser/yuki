package app.yuki.feature.library

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.database.PackageIndexStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.CategorySection
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import app.yuki.core.network.SearchQuery
import java.time.Instant
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
    private val versions = mutableMapOf<String, String>()
    private val changes = MutableStateFlow(0)

    override fun isPresent(packageName: String): Boolean = packageName in present

    override fun observeChanges(): Flow<Unit> = changes.map { }

    override fun launchIntentExists(packageName: String): Boolean = packageName in launchable

    override fun findAll(packageNames: List<String>): List<DevicePackage> =
        packageNames.filter { name -> name in present }.map { name ->
            DevicePackage(
                packageName = name,
                versionName = versions[name] ?: "1.0.0",
                versionCode = 1L,
                firstInstalledAt = Instant.EPOCH,
            )
        }

    fun install(packageName: String, isLaunchable: Boolean = true, versionName: String = "1.0.0") {
        present += packageName
        versions[packageName] = versionName
        if (isLaunchable) launchable += packageName
        changes.value += 1
    }

    fun uninstall(packageName: String) {
        present -= packageName
        launchable -= packageName
        versions -= packageName
        changes.value += 1
    }
}

internal class FakePackageIndexStore(
    private var entries: List<CatalogPackage> = emptyList(),
) : PackageIndexStore {
    override suspend fun packages(): List<CatalogPackage> = entries

    override suspend fun isEmpty(): Boolean = entries.isEmpty()

    override suspend fun replaceAll(packages: List<CatalogPackage>) {
        entries = packages
    }
}

internal class FakeCatalogRepository(
    private val result: Result<List<CatalogPackage>> = Result.success(emptyList()),
) : ListingRepository {
    override suspend fun browse(query: BrowseQuery): Result<ListingPage> =
        error("browse is not used by the library screen")

    override suspend fun featured(): Result<List<ListingSummary>> =
        error("featured is not used by the library screen")

    override suspend fun sections(limit: Int): Result<List<CategorySection>> =
        error("sections is not used by the library screen")

    override suspend fun search(query: SearchQuery): Result<List<ListingSummary>> =
        error("search is not used by the library screen")

    override suspend fun detail(slug: String): Result<ListingDetail> =
        error("detail is not used by the library screen")

    override suspend fun packages(): Result<List<CatalogPackage>> = result
}

internal fun libraryDependencies(
    store: FakeInstallStore,
    packages: FakeInstalledPackages,
    index: PackageIndexStore = FakePackageIndexStore(),
    repository: ListingRepository = FakeCatalogRepository(),
): LibraryDependencies = LibraryDependencies(
    reconciler = LibraryReconciler(store, packages),
    detection = PackageDetectionReconciler(store, index, repository, packages),
    packages = packages,
)

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
