package app.yuki.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val PAGE_INDICATOR_TAG = "pageIndicator"

private const val INACTIVE_DOT_ALPHA = 0.4f

@Composable
private fun PageIndicatorDot(isActive: Boolean, isAnimated: Boolean) {
    val targetWidth = if (isActive) YukiSize.IndicatorDotActive else YukiSize.IndicatorDot
    val targetColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = INACTIVE_DOT_ALPHA)
    }

    if (!isAnimated) {
        Box(
            modifier = Modifier
                .width(targetWidth)
                .height(YukiSize.IndicatorDot)
                .clip(YukiShape.Pill)
                .background(targetColor),
        )
        return
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = YukiMotion.spatial(),
        label = "pageIndicatorDotWidth",
    )
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = YukiMotion.fade(),
        label = "pageIndicatorDotColor",
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(YukiSize.IndicatorDot)
            .clip(YukiShape.Pill)
            .background(color),
    )
}

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true,
) {
    if (pageCount < 2) return

    Row(
        modifier = modifier
            .testTag(PAGE_INDICATOR_TAG)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            PageIndicatorDot(isActive = page == currentPage, isAnimated = isAnimated)
        }
    }
}
