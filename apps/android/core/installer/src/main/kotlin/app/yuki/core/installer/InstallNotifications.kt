package app.yuki.core.installer

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo

internal const val INSTALL_CHANNEL_ID = "installs"

internal fun installForegroundInfo(context: Context, target: InstallTarget): ForegroundInfo {
    ensureInstallChannel(context)

    val notification = NotificationCompat.Builder(context, INSTALL_CHANNEL_ID)
        .setSmallIcon(R.drawable.installer_ic_notification)
        .setContentTitle(
            context.getString(R.string.installer_notification_installing, target.title),
        )
        .setProgress(0, 0, true)
        .setOngoing(true)
        .setSilent(true)
        .build()

    return ForegroundInfo(
        target.githubRepoId.hashCode(),
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
}

private fun ensureInstallChannel(context: Context) {
    val channel = NotificationChannelCompat.Builder(
        INSTALL_CHANNEL_ID,
        NotificationManagerCompat.IMPORTANCE_LOW,
    )
        .setName(context.getString(R.string.installer_channel_installs))
        .build()

    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}
