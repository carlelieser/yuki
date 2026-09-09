package app.yuki.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import app.yuki.core.designsystem.theme.YukiMotion

private const val SLIDE_FRACTION = 6

private fun Int.inward() = this / SLIDE_FRACTION

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition =
    slideInHorizontally(animationSpec = YukiMotion.enter()) { width -> width.inward() } +
        fadeIn(animationSpec = YukiMotion.fade())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition =
    slideOutHorizontally(animationSpec = YukiMotion.exit()) { width -> -width.inward() } +
        fadeOut(animationSpec = YukiMotion.fade())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition =
    slideInHorizontally(animationSpec = YukiMotion.enter()) { width -> -width.inward() } +
        fadeIn(animationSpec = YukiMotion.fade())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition =
    slideOutHorizontally(animationSpec = YukiMotion.exit()) { width -> width.inward() } +
        fadeOut(animationSpec = YukiMotion.fade())

internal fun tabEnter(): EnterTransition = fadeIn(animationSpec = YukiMotion.fade())

internal fun tabExit(): ExitTransition = fadeOut(animationSpec = YukiMotion.fade())
