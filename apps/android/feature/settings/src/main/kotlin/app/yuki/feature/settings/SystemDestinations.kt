package app.yuki.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

const val SHIZUKU_WEBSITE = "https://shizuku.rikka.app"
const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

interface SystemDestinations {
    fun openShizukuWebsite()

    fun launchShizuku()

    fun openPermissionSettings(permission: String)
}

internal fun settingsIntentFor(permission: String, packageName: String): Intent {
    val appUri = Uri.fromParts("package", packageName, null)

    return when (permission) {
        Manifest.permission.REQUEST_INSTALL_PACKAGES ->
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, appUri)

        Manifest.permission.POST_NOTIFICATIONS ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri)
    }
}

internal class IntentSystemDestinations(private val context: Context) : SystemDestinations {
    override fun openShizukuWebsite() {
        start(Intent(Intent.ACTION_VIEW, SHIZUKU_WEBSITE.toUri()), "open $SHIZUKU_WEBSITE")
    }

    override fun launchShizuku() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: throw IllegalStateException("Shizuku is not launchable for package=$SHIZUKU_PACKAGE")

        start(intent, "launch package=$SHIZUKU_PACKAGE")
    }

    override fun openPermissionSettings(permission: String) {
        start(
            settingsIntentFor(permission, context.packageName),
            "open system settings for permission=$permission",
        )
    }

    private fun start(intent: Intent, operation: String) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (missing: ActivityNotFoundException) {
            throw IllegalStateException("No activity available to $operation", missing)
        }
    }
}

@Composable
fun rememberSystemDestinations(): SystemDestinations {
    val context = LocalContext.current
    return remember(context) { IntentSystemDestinations(context) }
}
