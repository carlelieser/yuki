package app.yuki.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.model.ListingSummary

const val CATALOG_SCREEN_TAG = "catalogScreen"

@Composable
fun CatalogRoute(
    onListingSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAuthorSelected: ((String) -> Unit)? = null,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val installs by viewModel.installs.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    CatalogScreen(
        installs = installs,
        listings = listings,
        callbacks = CatalogCallbacks(
            sort = sort,
            onSortSelected = viewModel::onSortChange,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
            onBackClick = onBackClick,
            onAuthorSelected = onAuthorSelected,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class CatalogCallbacks(
    val sort: BrowseSortOption,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onBackClick: () -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

@Composable
internal fun CatalogScreen(
    installs: ListingInstalls,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: CatalogCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = stringResource(R.string.search_catalog_title),
        onBackClick = callbacks.onBackClick,
        modifier = modifier.testTag(CATALOG_SCREEN_TAG),
        trailing = {
            SortSelector(
                selected = callbacks.sort,
                onSortSelected = callbacks.onSortSelected,
            )
        },
    ) {
        YukiPullToRefresh(
            isRefreshing = listings.isRefreshing,
            onRefresh = listings::refresh,
        ) {
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(BROWSE_LIST_TAG),
            ) {
                browseRefreshState(listings = listings)
                browseList(
                    listings = listings,
                    installs = installs,
                    onSelect = callbacks.onListingSelected,
                    onAuthorSelected = callbacks.onAuthorSelected,
                )
            }
        }
    }
}
