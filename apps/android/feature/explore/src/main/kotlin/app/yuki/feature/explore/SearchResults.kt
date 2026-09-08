package app.yuki.feature.explore

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
import app.yuki.core.designsystem.component.ClickableAppRow
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.toRowContent
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
    state: SearchState,
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
        when (results) {
            is UiState.Loading -> YukiLoadingIndicator(
                modifier = Modifier.padding(YukiSpacing.ExtraLarge),
            )

            is UiState.Failure -> FailureState(
                reason = results.reason,
                modifier = Modifier.padding(YukiSpacing.Large),
            )

            is UiState.Success -> MatchList(
                query = state.query,
                matches = results.data,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun MatchList(
    query: String,
    matches: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
) {
    if (matches.isEmpty()) {
        CollectionEmpty(
            content = noMatches(query),
            modifier = Modifier.padding(YukiSpacing.Large),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        items(items = matches, key = { listing -> listing.id }) { listing ->
            ClickableAppRow(
                content = listing.toRowContent(),
                onClick = { onSelect(listing) },
            )
        }
    }
}
