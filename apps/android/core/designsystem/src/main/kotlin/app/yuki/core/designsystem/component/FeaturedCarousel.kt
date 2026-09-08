package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary

@Composable
fun FeaturedCarousel(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        items(items = listings, key = { listing -> listing.id }) { listing ->
            FeaturedCard(listing = listing, onClick = { onSelect(listing) })
        }
    }
}

@Composable
fun ProductCarousel(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        items(items = listings, key = { listing -> listing.id }) { listing ->
            ProductCard(listing = listing, onClick = { onSelect(listing) })
        }
    }
}
