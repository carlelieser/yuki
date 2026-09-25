package app.yuki.core.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState

@Composable
fun SettingsMessage(message: String?, onShown: () -> Unit) {
    val hostState = LocalYukiSnackbarHostState.current

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect

        hostState.showSnackbar(message = text, withDismissAction = true)
        onShown()
    }
}
