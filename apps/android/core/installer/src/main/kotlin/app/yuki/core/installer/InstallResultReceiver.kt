package app.yuki.core.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal const val ACTION_INSTALL_STATUS = "app.yuki.core.installer.INSTALL_STATUS"

internal object InstallStatusBus {
    private val statuses = MutableSharedFlow<SessionStatus>(extraBufferCapacity = 16)

    val updates: SharedFlow<SessionStatus> = statuses.asSharedFlow()

    fun publish(status: SessionStatus) {
        statuses.tryEmit(status)
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        InstallStatusBus.publish(
            SessionStatus(
                sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
                code = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE,
                ),
                message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
                userAction = intent.userActionIntent(),
            ),
        )
    }
}

private fun Intent.userActionIntent(): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_INTENT)
    }
