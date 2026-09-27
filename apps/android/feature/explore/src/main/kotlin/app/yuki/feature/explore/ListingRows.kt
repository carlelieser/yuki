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
    val listings: UiState<List<ListingSummary>>,
    val installs: ListingInstalls,
)

internal fun LazyListScope.recencyRows(
    content: ExploreContent,
    titles: RowTitles,
    actions: ListingSectionActions,
) {
    listingRow(
        content = ListingRowContent(
            title = titles.newest,
            keyPrefix = NEWEST_ROW_KEY,
            listings = content.newest,
            installs = content.installs,
        ),
        actions = actions,
    )

    listingRow(
        content = ListingRowContent(
            title = titles.updated,
            keyPrefix = UPDATED_ROW_KEY,
            listings = content.updated,
            installs = content.installs,
        ),
        actions = actions,
    )
}

private fun LazyListScope.listingRow(
    content: ListingRowContent,
    actions: ListingSectionActions,
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
        actions = actions,
    )
}
