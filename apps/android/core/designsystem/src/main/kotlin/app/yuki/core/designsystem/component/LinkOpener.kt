package app.yuki.core.designsystem.component

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

fun interface LinkOpener {
    fun open(url: String)
}

internal fun openInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("No browser available to open url=$url", error)
    }
}

@Composable
fun rememberLinkOpener(): LinkOpener {
    val context = LocalContext.current
    return remember(context) { LinkOpener { url -> openInBrowser(context, url) } }
}
