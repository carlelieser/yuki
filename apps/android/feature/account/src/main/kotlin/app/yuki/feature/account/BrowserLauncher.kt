package app.yuki.feature.account

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
internal fun LaunchBrowser(url: String?, onLaunched: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        context.openInBrowserTab(url)
        onLaunched()
    }
}

private fun Context.openInBrowserTab(url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(this, url.toUri())
}
