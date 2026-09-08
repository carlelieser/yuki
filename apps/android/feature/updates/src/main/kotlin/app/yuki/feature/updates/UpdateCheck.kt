package app.yuki.feature.updates

import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.failureReason
import app.yuki.core.model.findUpdates
import app.yuki.core.network.ListingRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class CheckedListing(
    val app: InstalledApp,
    val detail: Result<ListingDetail>,
)

internal class UpdateCheck @Inject constructor(
    private val repository: ListingRepository,
) {
    suspend fun run(installs: List<InstalledApp>, includePrereleases: Boolean): UpdatesContent {
        val checked = fetchAll(installs)

        return UpdatesContent(
            updates = toUpdateRows(checked, includePrereleases),
            unchecked = checked.mapNotNull(::toUnchecked),
        )
    }

    private suspend fun fetchAll(installs: List<InstalledApp>): List<CheckedListing> =
        coroutineScope {
            installs
                .map { app -> async { CheckedListing(app, repository.detail(app.slug)) } }
                .map { pending -> pending.await() }
        }
}

private fun toUpdateRows(
    checked: List<CheckedListing>,
    includePrereleases: Boolean,
): List<UpdateRow> {
    val loaded = checked.mapNotNull { entry -> entry.detail.getOrNull() }
    val installs = checked.map(CheckedListing::app)
    val listings = loaded.associateBy(ListingDetail::githubRepoId)

    return findUpdates(installs, listings, includePrereleases)
        .map { update -> UpdateRow(update = update, install = update.toInstallState()) }
}

private fun toUnchecked(entry: CheckedListing): UncheckedApp? {
    val error = entry.detail.exceptionOrNull() ?: return null

    return UncheckedApp(app = entry.app, reason = error.failureReason())
}
