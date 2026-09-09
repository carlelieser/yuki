package app.yuki.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object YukiMotion {
    const val ScreenMillis = 220
    const val ScreenExitMillis = 180
    const val FadeMillis = 140

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> enter() = tween<T>(durationMillis = ScreenMillis, easing = Emphasized)

    fun <T> exit() = tween<T>(durationMillis = ScreenExitMillis, easing = Accelerate)

    fun <T> fade() = tween<T>(durationMillis = FadeMillis, easing = Emphasized)
}
