package app.yuki.core.designsystem.component

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.theme.YukiMotion

@OptIn(ExperimentalSharedTransitionApi::class)
class ScreenshotTransition(
    val sharedScope: SharedTransitionScope,
    val contentScope: AnimatedContentScope,
)

val LocalScreenshotTransition = compositionLocalOf<ScreenshotTransition?> { null }

fun screenshotSharedKey(url: String): String = "screenshot-$url"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedScreenshot(url: String): Modifier {
    val transition = LocalScreenshotTransition.current ?: return this
    if (!LocalScreenshotFocus.current.isFocused(url)) return this

    return with(transition.sharedScope) {
        this@sharedScreenshot.sharedElement(
            sharedContentState = rememberSharedContentState(key = screenshotSharedKey(url)),
            animatedVisibilityScope = transition.contentScope,
            boundsTransform = { _, _ -> YukiMotion.spatial() },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ScreenshotTransitionScope(
    sharedScope: SharedTransitionScope,
    contentScope: AnimatedContentScope,
    content: @Composable () -> Unit,
) {
    val transition = ScreenshotTransition(sharedScope = sharedScope, contentScope = contentScope)

    CompositionLocalProvider(LocalScreenshotTransition provides transition, content = content)
}

@Composable
fun ScreenshotFocusScope(focus: ScreenshotFocus, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalScreenshotFocus provides focus, content = content)
}
