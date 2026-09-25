package app.yuki.core.installer

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal interface UserActionLauncher {
    fun launch(sessionId: Int, intent: Intent)

    fun dismiss(sessionId: Int)
}

internal const val CONFIRMATION_NOTIFICATION_TAG = "install_confirmation"

internal class UserActionPrompt(private val context: Context) : UserActionLauncher {
    override fun launch(sessionId: Int, intent: Intent) {
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        post(sessionId, intent)
    }

    override fun dismiss(sessionId: Int) {
        NotificationManagerCompat.from(context).cancel(CONFIRMATION_NOTIFICATION_TAG, sessionId)
    }

    fun post(sessionId: Int, intent: Intent) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isAllowed = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (!isAllowed) return

        ensureChannel(context, InstallChannel.Confirmations)
        NotificationManagerCompat.from(context).notify(
            CONFIRMATION_NOTIFICATION_TAG,
            sessionId,
            confirmation(sessionId, intent),
        )
    }

    private fun confirmation(sessionId: Int, intent: Intent) =
        NotificationCompat.Builder(context, CONFIRMATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.installer_ic_notification)
            .setContentTitle(context.getString(R.string.installer_notification_confirm_title))
            .setContentText(context.getString(R.string.installer_notification_confirm_text))
            .setContentIntent(confirmIntent(sessionId, intent))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    private fun confirmIntent(sessionId: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            sessionId,
            Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
