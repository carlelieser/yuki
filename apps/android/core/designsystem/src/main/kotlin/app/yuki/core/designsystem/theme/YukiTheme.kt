package app.yuki.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
private fun dynamicColors(isDarkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val context = LocalContext.current
    return if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
private fun yukiColorScheme(isDarkTheme: Boolean, isDynamicColorEnabled: Boolean): ColorScheme {
    val dynamic = if (isDynamicColorEnabled) dynamicColors(isDarkTheme) else null
    if (dynamic != null) return dynamic

    return if (isDarkTheme) YukiDarkColors else YukiLightColors
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YukiTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColorEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = yukiColorScheme(isDarkTheme, isDynamicColorEnabled),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
