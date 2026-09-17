package app.yuki.feature.listing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

fun interface ObtainiumOpener {
    fun open(repositoryUrl: String)
}

const val OBTAINIUM_LABEL = "Open in Obtainium"

private val OBTAINIUM_PACKAGES = listOf(
    "dev.imranr.obtainium",
    "dev.imranr.obtainium.fdroid",
    "dev.imranr.obtainium.googleplay",
)

internal fun obtainiumAddUrl(repositoryUrl: String): String = "obtainium://add/$repositoryUrl"

internal fun isObtainiumInstalled(context: Context): Boolean =
    OBTAINIUM_PACKAGES.any { packageName -> isPackagePresent(context, packageName) }

private fun isPackagePresent(context: Context, packageName: String): Boolean = try {
    context.packageManager.getPackageInfo(packageName, 0)
    true
} catch (absent: PackageManager.NameNotFoundException) {
    false
}

internal fun startObtainium(context: Context, repositoryUrl: String) {
    val url = obtainiumAddUrl(repositoryUrl)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("No target available to open url=$url", error)
    }
}

@Composable
fun rememberObtainiumOpener(): ObtainiumOpener {
    val context = LocalContext.current
    return remember(context) { ObtainiumOpener { url -> startObtainium(context, url) } }
}

@Composable
internal fun rememberIsObtainiumInstalled(): Boolean {
    val context = LocalContext.current
    return remember(context) { isObtainiumInstalled(context) }
}
