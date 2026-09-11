package app.yuki.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.yuki.core.designsystem.component.ShimmerBox
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val FEATURED_ROW_PLACEHOLDER_TAG = "featuredRowPlaceholder"

private const val PLACEHOLDER_CARD_COUNT = 2
private const val PLACEHOLDER_TITLE_WIDTH_FRACTION = 0.6f

@Composable
internal fun FeaturedRowPlaceholder(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier
            .testTag(FEATURED_ROW_PLACEHOLDER_TAG)
            .clearAndSetSemantics { },
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        userScrollEnabled = false,
    ) {
        items(PLACEHOLDER_CARD_COUNT) { FeaturedCardPlaceholder() }
    }
}

@Composable
private fun FeaturedCardPlaceholder() {
    Column(modifier = Modifier.width(YukiSize.BannerWidth)) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(YukiRatio.Banner),
            shape = YukiShape.Card,
        )
        FeaturedIdentityPlaceholder()
    }
}

@Composable
private fun FeaturedIdentityPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(
            modifier = Modifier.size(YukiSize.IconMedium),
            shape = YukiShape.Icon,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(PLACEHOLDER_TITLE_WIDTH_FRACTION)
                    .height(YukiSize.TitleLineHeight),
            )
            ShimmerBox(
                modifier = Modifier
                    .width(YukiSize.BadgeWidth)
                    .height(YukiSize.BadgeHeight),
            )
        }
    }
}
