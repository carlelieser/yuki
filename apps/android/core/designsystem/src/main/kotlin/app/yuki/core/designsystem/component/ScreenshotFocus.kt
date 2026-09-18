package app.yuki.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class ScreenshotFocus {
    var url: String? by mutableStateOf(null)
        private set

    fun focus(url: String) {
        this.url = url
    }

    fun release() {
        url = null
    }

    fun confine(urls: List<String>) {
        if (url !in urls) url = null
    }

    fun isFocused(url: String): Boolean = this.url == null || this.url == url
}

val LocalScreenshotFocus = staticCompositionLocalOf { ScreenshotFocus() }
