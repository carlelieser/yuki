package app.yuki.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

private enum class CardMediaKind(val ratio: Float, val hasGlyph: Boolean) {
    Icon(ratio = YukiRatio.Square, hasGlyph = true),
    Banner(ratio = YukiRatio.Banner, hasGlyph = false),
}

@Composable
private fun CardMediaPlaceholder(kind: CardMediaKind) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(kind.ratio),
        contentAlignment = Alignment.Center,
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(kind.ratio),
            isAnimated = false,
        )

        if (!kind.hasGlyph) return@Box

        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(YukiSize.IconLarge)
                .clearAndSetSemantics { },
        )
    }
}

@Composable
private fun CardMedia(imageUrl: String?, kind: CardMediaKind) {
    if (imageUrl == null) {
        CardMediaPlaceholder(kind = kind)
        return
    }

    val painter = rememberAsyncImagePainter(model = imageUrl, contentScale = ContentScale.Crop)
    val state = painter.state.collectAsState().value

    if (state !is AsyncImagePainter.State.Success) {
        CardMediaPlaceholder(kind = kind)
        return
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(kind.ratio)
            .clearAndSetSemantics { },
    )
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
        CardMedia(imageUrl = listing.iconUrl, kind = CardMediaKind.Icon)
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
        CardMedia(imageUrl = listing.bannerUrl, kind = CardMediaKind.Banner)
        WideCaption(listing = listing)
    }
}
