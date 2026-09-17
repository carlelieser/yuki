package app.yuki.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabToTab(): Boolean {
    val initialTab = initialState.destination.selectedTab()
    val targetTab = targetState.destination.selectedTab()

    return initialTab != null && targetTab != null
}

private fun peerEnter(): EnterTransition =
    fadeIn(animationSpec = YukiMotion.fade()) +
        scaleIn(
            initialScale = YukiMotion.TabScaleEnter,
            animationSpec = YukiMotion.fade(),
        )

private fun peerExit(): ExitTransition =
    fadeOut(animationSpec = YukiMotion.fade()) +
        scaleOut(
            targetScale = YukiMotion.TabScaleExit,
            animationSpec = YukiMotion.fade(),
        )

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition =
    if (isTabToTab()) peerEnter() else forwardEnter()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition =
    if (isTabToTab()) peerExit() else forwardExit()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tabPopEnter(): EnterTransition =
    if (isTabToTab()) peerEnter() else backEnter()

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tabPopExit(): ExitTransition =
    if (isTabToTab()) peerExit() else backExit()
