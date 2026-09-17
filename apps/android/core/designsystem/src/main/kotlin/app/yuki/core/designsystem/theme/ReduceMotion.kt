package app.yuki.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReduceMotion = compositionLocalOf { false }

fun isReduceMotionEnabled(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1f,
    )

    return scale == 0f
}

@Composable
fun rememberReduceMotion(): Boolean = isReduceMotionEnabled(LocalContext.current)

@Composable
@ReadOnlyComposable
internal fun reduceMotion(): Boolean = LocalReduceMotion.current
