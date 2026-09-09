package app.yuki.feature.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.component.AppIcon
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import coil3.compose.AsyncImage

const val LISTING_BANNER_TAG = "listingBanner"

@Composable
internal fun ListingBanner(bannerUrl: String?, modifier: Modifier = Modifier) {
    val shape = modifier
        .fillMaxWidth()
        .aspectRatio(YukiRatio.Banner)
        .clip(YukiShape.Media)
        .testTag(LISTING_BANNER_TAG)

    if (bannerUrl == null) {
        Column(modifier = shape.background(MaterialTheme.colorScheme.surfaceContainerHighest)) { }
        return
    }

    AsyncImage(
        model = bannerUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = shape,
    )
}

@Composable
private fun ListingIdentity(summary: ListingSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = summary.author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ListingStars(stars = summary.stars)
    }
}

@Composable
private fun ListingStars(stars: Int) {
    Text(
        text = "$stars stars",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun ListingHeader(summary: ListingSummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(iconUrl = summary.iconUrl, size = YukiSize.IconExtraLarge)
        ListingIdentity(summary = summary, modifier = Modifier.weight(1f))
    }
}
