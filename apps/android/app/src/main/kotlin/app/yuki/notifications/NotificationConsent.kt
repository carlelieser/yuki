package app.yuki.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal object NotificationConsent {
    val isRequired: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    const val PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

    fun isGranted(context: Context): Boolean {
        if (!isRequired) return true

        val status = ContextCompat.checkSelfPermission(context, PERMISSION)
        return status == PackageManager.PERMISSION_GRANTED
    }
}
