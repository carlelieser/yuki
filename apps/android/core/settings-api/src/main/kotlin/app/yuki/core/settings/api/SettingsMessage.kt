package app.yuki.core.settings.api

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

val LocalSettingsSnackbar: ProvidableCompositionLocal<SnackbarHostState> =
    compositionLocalOf { error("No settings snackbar host provided") }

@Composable
fun ProvideSettingsSnackbar(
    hostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSettingsSnackbar provides hostState, content = content)
}

@Composable
fun SettingsMessage(message: String?, onShown: () -> Unit) {
    val hostState = LocalSettingsSnackbar.current

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect

        hostState.showSnackbar(message = text, withDismissAction = true)
        onShown()
    }
}
