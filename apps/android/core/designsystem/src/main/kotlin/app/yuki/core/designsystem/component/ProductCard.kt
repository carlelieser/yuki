package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import coil3.compose.AsyncImage

@Composable
private fun CardMedia(imageUrl: String?, ratio: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().aspectRatio(ratio), isAnimated = false)
            return@Box
        }

        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clearAndSetSemantics { },
        )
    }
}

@Composable
private fun CardCaption(listing: ListingSummary) {
    Column(
        modifier = Modifier.padding(YukiSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listing.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProductCard(
    listing: ListingSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = YukiShape.Card,
        modifier = modifier.width(YukiSize.CardWidth),
    ) {
        CardMedia(imageUrl = listing.iconUrl, ratio = YukiRatio.Square)
        CardCaption(listing = listing)
    }
}

@Composable
private fun WideCaption(listing: ListingSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(iconUrl = listing.iconUrl, size = YukiSize.IconMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listing.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun FeaturedCard(
    listing: ListingSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = YukiShape.Card,
        modifier = modifier.width(YukiSize.BannerWidth),
    ) {
        CardMedia(imageUrl = listing.bannerUrl, ratio = YukiRatio.Banner)
        WideCaption(listing = listing)
    }
}
