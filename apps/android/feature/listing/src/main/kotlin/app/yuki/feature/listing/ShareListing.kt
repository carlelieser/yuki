package app.yuki.feature.listing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

const val SHARE_DESCRIPTION = "Share"

fun interface ListingSharer {
    fun share(url: String)
}

internal fun listingShareUrl(baseUrl: String, slug: String): String =
    "${baseUrl.trimEnd('/')}/listings/$slug"

internal fun shareUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, url)

    val chooser = Intent.createChooser(intent, SHARE_DESCRIPTION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(chooser)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("No target available to share url=$url", error)
    }
}

@Composable
fun rememberListingSharer(): ListingSharer {
    val context = LocalContext.current
    return remember(context) { ListingSharer { url -> shareUrl(context, url) } }
}
