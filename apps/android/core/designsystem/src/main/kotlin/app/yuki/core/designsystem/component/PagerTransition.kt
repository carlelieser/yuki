package app.yuki.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import app.yuki.core.designsystem.theme.YukiMotion
import kotlin.math.absoluteValue

internal fun pageTransitionFraction(offsetFromCurrent: Float): Float =
    offsetFromCurrent.absoluteValue.coerceIn(0f, 1f)

fun Modifier.pagerPageTransition(
    offsetFromCurrent: () -> Float,
    isAnimated: Boolean = true,
): Modifier {
    if (!isAnimated) return this

    return graphicsLayer {
        val fraction = pageTransitionFraction(offsetFromCurrent())
        val scale = lerp(1f, YukiMotion.CarouselPageScaleMinimum, fraction)

        scaleX = scale
        scaleY = scale
        alpha = lerp(1f, YukiMotion.CarouselPageAlphaMinimum, fraction)
    }
}
