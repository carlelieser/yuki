package app.yuki.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import app.yuki.core.designsystem.theme.YukiShape

private const val SHIMMER_PERIOD_MILLIS = 900
private const val SHIMMER_MINIMUM_ALPHA = 0.35f
private const val SHIMMER_MAXIMUM_ALPHA = 0.75f

@Composable
private fun shimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = SHIMMER_MINIMUM_ALPHA,
        targetValue = SHIMMER_MAXIMUM_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_PERIOD_MILLIS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return alpha
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = YukiShape.Media,
    isAnimated: Boolean = true,
) {
    val alpha = if (isAnimated) shimmerAlpha() else SHIMMER_MINIMUM_ALPHA

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)),
    )
}
