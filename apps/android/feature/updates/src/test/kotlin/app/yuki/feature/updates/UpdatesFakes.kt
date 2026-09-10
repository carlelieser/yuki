package app.yuki.feature.updates

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.CategorySection
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingLinks
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.ListingVersion
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class FakeInstallStore(private val initial: List<InstalledApp>) : InstallStore {
    private val rows = MutableStateFlow(initial)

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
        rows.value = rows.value.filterNot { app -> app.packageName in packageNames }
    }
}

internal class FakeListingRepository(
    private val details: Map<String, Result<ListingDetail>>,
) : ListingRepository {
    val requested: MutableList<String> = mutableListOf()

    override suspend fun browse(query: BrowseQuery): Result<ListingPage> =
        Result.failure(IllegalStateException("browse is not used by Updates"))

    override suspend fun featured(): Result<List<ListingSummary>> =
        Result.failure(IllegalStateException("featured is not used by Updates"))

    override suspend fun sections(limit: Int): Result<List<CategorySection>> =
        Result.failure(IllegalStateException("sections is not used by Updates"))

    override suspend fun search(query: String): Result<List<ListingSummary>> =
        Result.failure(IllegalStateException("search is not used by Updates"))

    override suspend fun detail(slug: String): Result<ListingDetail> {
        requested += slug
        return details[slug] ?: Result.failure(IllegalStateException("No fake detail for $slug"))
    }
}

internal class RecordingUpdateInstaller : UpdateInstaller {
    private val progress = MutableSharedFlow<InstallState>(replay = 8)

    val requested: MutableList<AvailableUpdate> = mutableListOf()

    suspend fun emit(next: InstallState) {
        progress.emit(next)
    }

    override fun install(update: AvailableUpdate): Flow<InstallState> {
        requested += update
        return progress.asSharedFlow()
    }
}

internal fun installedApp(
    githubRepoId: Long,
    slug: String,
    versionTag: String,
): InstalledApp = InstalledApp(
    githubRepoId = githubRepoId,
    packageName = "app.$slug",
    slug = slug,
    title = slug.replaceFirstChar(Char::uppercase),
    iconUrl = null,
    versionTag = versionTag,
)

internal fun listingDetail(
    githubRepoId: Long,
    slug: String,
    versions: List<ListingVersion>,
): ListingDetail = ListingDetail(
    summary = ListingSummary(
        id = "id-$githubRepoId",
        githubRepoId = githubRepoId,
        slug = slug,
        title = slug.replaceFirstChar(Char::uppercase),
        author = "author",
        description = null,
        iconUrl = null,
        bannerUrl = null,
        category = null,
        stars = 0,
    ),
    links = ListingLinks(
        authorUrl = "https://example.test/author",
        repositoryUrl = "https://example.test/$slug",
        homepageUrl = null,
    ),
    license = null,
    isArchived = false,
    screenshots = emptyList(),
    versions = versions,
)

internal fun version(
    tag: String,
    isPrerelease: Boolean = false,
    downloadUrl: String? = "https://example.test/$tag.apk",
): ListingVersion = ListingVersion(
    tag = tag,
    name = tag,
    downloadUrl = downloadUrl,
    assetName = "$tag.apk",
    isPrerelease = isPrerelease,
    publishedAt = null,
)
