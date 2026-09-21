package app.yuki.core.designsystem.theme

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

val LocalTouchExploration = compositionLocalOf { false }

fun isTouchExplorationEnabled(context: Context): Boolean =
    context.getSystemService<AccessibilityManager>()?.isTouchExplorationEnabled == true

@Composable
fun rememberTouchExploration(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService<AccessibilityManager>() }
    var isEnabled by remember(manager) {
        mutableStateOf(manager?.isTouchExplorationEnabled == true)
    }

    DisposableEffect(manager) {
        if (manager == null) return@DisposableEffect onDispose { }

        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            isEnabled = enabled
        }

        isEnabled = manager.isTouchExplorationEnabled
        manager.addTouchExplorationStateChangeListener(listener)

        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }

    return isEnabled
}
