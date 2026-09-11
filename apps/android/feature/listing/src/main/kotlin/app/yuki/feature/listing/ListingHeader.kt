package app.yuki.feature.listing

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.ListingIdentity
import app.yuki.core.designsystem.component.ListingIdentityVariant
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import coil3.compose.AsyncImage

const val LISTING_BANNER_TAG = "listingBanner"

@Composable
internal fun ListingBanner(bannerUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = bannerUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(YukiRatio.Banner)
            .clip(YukiShape.Media)
            .testTag(LISTING_BANNER_TAG),
    )
}

@Composable
internal fun ListingHeader(summary: ListingSummary, modifier: Modifier = Modifier) {
    ListingIdentity(
        listing = summary,
        modifier = modifier.padding(horizontal = YukiSpacing.Large),
        variant = ListingIdentityVariant.Detail,
    )
}
