package app.yuki.feature.explore

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.ListingSectionActions
import app.yuki.core.designsystem.component.ListingSectionContent
import app.yuki.core.designsystem.component.listingSection
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

internal const val NEWEST_SORT = "newest"
internal const val UPDATED_SORT = "updated"

private const val NEWEST_ROW_KEY = "newestRow"
private const val UPDATED_ROW_KEY = "updatedRow"

internal data class RowTitles(
    val newest: String,
    val updated: String,
)

@Composable
internal fun rowTitles(): RowTitles = RowTitles(
    newest = stringResource(R.string.explore_new_title),
    updated = stringResource(R.string.explore_updated_title),
)

private data class ListingRowContent(
    val title: String,
    val keyPrefix: String,
    val sort: String,
    val listings: UiState<List<ListingSummary>>,
    val installs: ListingInstalls,
)

internal fun LazyListScope.recencyRows(
    content: ExploreContent,
    titles: RowTitles,
    callbacks: ExploreCallbacks,
) {
    listingRow(
        content = ListingRowContent(
            title = titles.newest,
            keyPrefix = NEWEST_ROW_KEY,
            sort = NEWEST_SORT,
            listings = content.newest,
            installs = content.installs,
        ),
        callbacks = callbacks,
    )

    listingRow(
        content = ListingRowContent(
            title = titles.updated,
            keyPrefix = UPDATED_ROW_KEY,
            sort = UPDATED_SORT,
            listings = content.updated,
            installs = content.installs,
        ),
        callbacks = callbacks,
    )
}

private fun LazyListScope.listingRow(
    content: ListingRowContent,
    callbacks: ExploreCallbacks,
) {
    val listings = content.listings
    if (listings !is UiState.Success) return
    if (listings.data.isEmpty()) return

    listingSection(
        content = ListingSectionContent(
            title = content.title,
            keyPrefix = content.keyPrefix,
            listings = listings.data,
            installs = content.installs,
        ),
        actions = ListingSectionActions(
            onListingSelected = callbacks.onListingSelected,
            onSeeAll = callbacks.onSeeAllSelected?.let { onSeeAll -> { onSeeAll(content.sort) } },
            onAuthorSelected = callbacks.onAuthorSelected,
        ),
    )
}
