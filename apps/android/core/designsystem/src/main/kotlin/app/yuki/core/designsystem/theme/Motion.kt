package app.yuki.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object YukiMotion {
    const val ScreenMillis = 220
    const val ScreenExitMillis = 180
    const val FadeMillis = 200
    const val ResizeMillis = 200

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val TabScaleEnter = 0.97f
    const val TabScaleExit = 1.03f

    const val CarouselAdvanceMillis = 4_000L
    const val CarouselPageScaleMinimum = 0.92f
    const val CarouselPageAlphaMinimum = 0.6f

    @Volatile
    var isReduced: Boolean = false
        internal set

    private fun <T> timed(durationMillis: Int, easing: CubicBezierEasing): FiniteAnimationSpec<T> =
        if (isReduced) snap() else tween(durationMillis = durationMillis, easing = easing)

    fun <T> enter(): FiniteAnimationSpec<T> = timed(ScreenMillis, Emphasized)

    fun <T> exit(): FiniteAnimationSpec<T> = timed(ScreenExitMillis, Accelerate)

    fun <T> fade(): FiniteAnimationSpec<T> = timed(FadeMillis, Emphasized)

    fun <T> resize(): FiniteAnimationSpec<T> = timed(ResizeMillis, Emphasized)

    fun <T> stateFade(): FiniteAnimationSpec<T> = timed(FadeMillis, Emphasized)

    fun <T> progress(): FiniteAnimationSpec<T> =
        if (isReduced) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        }

    fun <T> spatial(): FiniteAnimationSpec<T> =
        if (isReduced) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        }
}
