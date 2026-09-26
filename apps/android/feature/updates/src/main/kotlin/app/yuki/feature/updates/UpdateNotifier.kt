package app.yuki.feature.updates

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.yuki.core.model.InstalledApp
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import app.yuki.core.designsystem.R as DesignR

const val EXTRA_OPEN_UPDATES = "app.yuki.extra.OPEN_UPDATES"

private const val UPDATES_CHANNEL_ID = "updates"
private const val UPDATES_NOTIFICATION_TAG = "updates_available"
private const val UPDATES_NOTIFICATION_ID = 1

internal interface UpdateNotifier {
    fun show(apps: List<InstalledApp>): Boolean

    fun dismiss()
}

internal class SystemUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateNotifier {
    private val notifications = NotificationManagerCompat.from(context)

    override fun show(apps: List<InstalledApp>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val status = ContextCompat.checkSelfPermission(context, permission)
            if (status != PackageManager.PERMISSION_GRANTED) return false
        }

        ensureChannel()
        notifications.notify(UPDATES_NOTIFICATION_TAG, UPDATES_NOTIFICATION_ID, summary(apps))
        return true
    }

    override fun dismiss() {
        notifications.cancel(UPDATES_NOTIFICATION_TAG, UPDATES_NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(
            UPDATES_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        )
            .setName(context.getString(R.string.updates_channel_name))
            .build()

        notifications.createNotificationChannel(channel)
    }

    private fun summary(apps: List<InstalledApp>) =
        NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(DesignR.drawable.ic_update)
            .setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.updates_notification_title,
                    apps.size,
                    apps.size,
                ),
            )
            .setContentText(apps.joinToString(separator = ", ", transform = InstalledApp::title))
            .setNumber(apps.size)
            .setContentIntent(openUpdates())
            .setAutoCancel(true)
            .build()

    private fun openUpdates(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        val intent = launch
            .putExtra(EXTRA_OPEN_UPDATES, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            UPDATES_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateNotifierModule {
    @Binds
    abstract fun bindNotifier(notifier: SystemUpdateNotifier): UpdateNotifier
}
