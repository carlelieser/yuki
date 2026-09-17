package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.toProductListItemContent
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val SEARCH_RESULTS_TAG = "searchResults"

private fun noMatches(query: String) = EmptyContent(
    title = "No matches",
    description = "Nothing matched \"$query\". Try a different search.",
)

@Composable
internal fun SearchResults(
    state: SearchContent,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = state.results ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SEARCH_RESULTS_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YukiAnimatedState(state = results) { settled ->
            when (settled) {
                is UiState.Loading -> YukiLoadingIndicator(
                    modifier = Modifier.padding(YukiSpacing.ExtraLarge),
                )

                is UiState.Failure -> FailureState(
                    reason = settled.reason,
                    modifier = Modifier.padding(YukiSpacing.Large),
                )

                is UiState.Success -> MatchList(
                    matches = Matches(query = state.query, listings = settled.data),
                    installs = state.installs,
                    onSelect = onSelect,
                )
            }
        }
    }
}

private data class Matches(
    val query: String,
    val listings: List<ListingSummary>,
)

@Composable
private fun MatchList(
    matches: Matches,
    installs: ListingInstalls,
    onSelect: (ListingSummary) -> Unit,
) {
    if (matches.listings.isEmpty()) {
        CollectionEmpty(
            content = noMatches(matches.query),
            modifier = Modifier.padding(YukiSpacing.Large),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        items(items = matches.listings, key = { listing -> listing.id }) { listing ->
            ClickableProductListItem(
                content = installs.apply(listing, listing.toProductListItemContent()),
                onClick = { onSelect(listing) },
            )
        }
    }
}
