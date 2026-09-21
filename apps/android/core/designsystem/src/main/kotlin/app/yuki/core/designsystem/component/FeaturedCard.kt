package app.yuki.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            .aspectRatio(kind.ratio)
            .clip(YukiShape.Card),
        contentAlignment = Alignment.Center,
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(kind.ratio),
            shape = YukiShape.Card,
            isAnimated = false,
        )

        if (!kind.hasGlyph) return@Box

        Icon(
            imageVector = YukiIcons.DeployedCode,
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
            .clip(YukiShape.Card)
            .clearAndSetSemantics { },
    )
}

private fun Modifier.cardDescription(contentDescription: String?): Modifier {
    if (contentDescription == null) return this

    return semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
    }
}

@Composable
fun FeaturedCard(
    listing: ListingSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
    onAuthorClick: ((String) -> Unit)? = null,
    contentDescription: String? = null,
) {
    Card(
        onClick = onClick,
        shape = YukiShape.Card,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .cardDescription(contentDescription),
    ) {
        CardMedia(imageUrl = listing.bannerUrl, kind = CardMediaKind.Banner)
        ListingIdentity(
            listing = listing,
            modifier = Modifier.padding(vertical = YukiSpacing.Medium),
            isInstalled = isInstalled,
            onAuthorClick = onAuthorClick,
        )
    }
}
