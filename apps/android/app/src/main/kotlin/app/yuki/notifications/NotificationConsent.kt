package app.yuki.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat

internal object NotificationConsent {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isRequired: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(context: Context): Boolean {
        if (!isRequired) return true

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val status = ContextCompat.checkSelfPermission(context, permission)
        return status == PackageManager.PERMISSION_GRANTED
    }

    fun request(launcher: ActivityResultLauncher<String>) {
        if (!isRequired) return

        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
