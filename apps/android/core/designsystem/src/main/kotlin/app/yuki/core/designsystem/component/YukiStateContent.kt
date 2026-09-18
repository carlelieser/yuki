package app.yuki.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.model.UiState

private const val STATE_SCALE_ENTER = 0.98f

private enum class UiStatePhase {
    Loading,
    Success,
    Failure,
}

private fun UiState<*>.phase(): UiStatePhase = when (this) {
    UiState.Loading -> UiStatePhase.Loading
    is UiState.Success -> UiStatePhase.Success
    is UiState.Failure -> UiStatePhase.Failure
}

private fun stateTransform(): ContentTransform {
    val enter = fadeIn(animationSpec = YukiMotion.stateFade()) +
        scaleIn(initialScale = STATE_SCALE_ENTER, animationSpec = YukiMotion.stateFade())

    return enter togetherWith fadeOut(animationSpec = YukiMotion.stateFade())
}

@Composable
fun <T> YukiAnimatedState(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(UiState<T>) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { stateTransform() },
        contentKey = { it.phase() },
        modifier = modifier,
        label = "uiState",
        content = { settled -> content(settled) },
    )
}
