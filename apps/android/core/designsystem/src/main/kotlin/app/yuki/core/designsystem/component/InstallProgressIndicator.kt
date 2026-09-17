package app.yuki.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.theme.YukiMotion

@Composable
internal fun animatedProgress(fraction: Float): Float {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = YukiMotion.progress(),
        label = "installProgress",
    )

    return animated
}

@Composable
internal fun ProgressIndicatorCrossfade(
    fraction: Float?,
    modifier: Modifier = Modifier,
    indicator: @Composable (Float?) -> Unit,
) {
    Crossfade(
        targetState = fraction != null,
        animationSpec = YukiMotion.fade(),
        modifier = modifier,
        label = "installProgressShape",
    ) { isDeterminate ->
        indicator(if (isDeterminate) fraction else null)
    }
}
