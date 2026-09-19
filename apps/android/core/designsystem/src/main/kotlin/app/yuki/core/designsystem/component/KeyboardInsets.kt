package app.yuki.core.designsystem.component

import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val SETTLE_MILLIS = 285
private const val PROGRESS_GRACE_FRAMES = 2

private val KeyboardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val LocalKeyboardInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun ProvideKeyboardInsets(content: @Composable () -> Unit) {
    val host = LocalView.current.parent as? View
    val scope = rememberCoroutineScope()
    val tracker = remember(host) { host?.let { view -> KeyboardInsetTracker(view, scope) } }

    DisposableEffect(tracker) {
        tracker?.attach()
        onDispose { tracker?.detach() }
    }

    CompositionLocalProvider(LocalKeyboardInsets provides tracker?.insets, content = content)
}

@Composable
fun keyboardInsets(): WindowInsets = LocalKeyboardInsets.current ?: WindowInsets.ime

private class KeyboardInsetTracker(
    private val host: View,
    private val scope: CoroutineScope,
) : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE),
    OnApplyWindowInsetsListener {

    private var bottom by mutableIntStateOf(0)
    private var isAnimating = false
    private var expectedBottom: Int? = null
    private var settling: Job? = null
    private var settlingTarget = 0

    val insets: WindowInsets = object : WindowInsets {
        override fun getLeft(density: Density, layoutDirection: LayoutDirection) = 0
        override fun getTop(density: Density) = 0
        override fun getRight(density: Density, layoutDirection: LayoutDirection) = 0
        override fun getBottom(density: Density) = bottom
    }

    fun attach() {
        bottom = ViewCompat.getRootWindowInsets(host).keyboardBottom()
        ViewCompat.setOnApplyWindowInsetsListener(host, this)
        ViewCompat.setWindowInsetsAnimationCallback(host, this)
        host.requestApplyInsets()
    }

    fun detach() {
        settling?.cancel()
        ViewCompat.setOnApplyWindowInsetsListener(host, null)
        ViewCompat.setWindowInsetsAnimationCallback(host, null)
    }

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        isAnimating = true
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat,
    ): WindowInsetsAnimationCompat.BoundsCompat {
        expectedBottom?.let { expected -> settleTo(expected, graceFrames = PROGRESS_GRACE_FRAMES) }
        return bounds
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        settling?.cancel()
        bottom = insets.keyboardBottom()
        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        isAnimating = false
        expectedBottom = null
        settleTo(ViewCompat.getRootWindowInsets(host).keyboardBottom())
        host.post { host.requestApplyInsets() }
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        if (isAnimating) {
            expectedBottom = insets.keyboardBottom()
        } else {
            settleTo(insets.keyboardBottom())
        }
        return insets
    }

    private fun settleTo(target: Int, graceFrames: Int = 0) {
        val isAlreadyHeadingThere = settling?.isActive == true && settlingTarget == target
        if (isAlreadyHeadingThere) return

        settling?.cancel()
        if (target == bottom) return

        settlingTarget = target
        settling = scope.launch {
            repeat(graceFrames) { withFrameNanos { } }
            animate(
                initialValue = bottom.toFloat(),
                targetValue = target.toFloat(),
                animationSpec = tween(SETTLE_MILLIS, easing = KeyboardEasing),
            ) { value, _ -> bottom = value.roundToInt() }
        }
    }
}

private fun WindowInsetsCompat?.keyboardBottom(): Int =
    this?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
