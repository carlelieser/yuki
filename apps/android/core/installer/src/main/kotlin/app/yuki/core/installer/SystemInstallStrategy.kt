package app.yuki.core.installer

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.transformWhile

internal fun interface UserActionLauncher {
    fun launch(intent: Intent)
}

internal class SystemInstallStrategy(
    private val sessions: InstallSessions,
    private val launcher: UserActionLauncher,
) : InstallStrategy {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        sessions = InstallSessionWriter(context),
        launcher = UserActionLauncher { intent ->
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )

    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        val sessionId = sessions.createSession(identity)
        sessions.writeApk(sessionId, apk)

        val outcomes = InstallStatusBus.updates
            .onSubscription { sessions.commit(sessionId) }
            .filter { status -> status.sessionId == sessionId }
            .transformWhile { status -> emitUntilTerminal(status) }

        emitAll(outcomes)
    }

    private suspend fun FlowCollector<InstallOutcome>.emitUntilTerminal(
        status: SessionStatus,
    ): Boolean {
        val outcome = status.toOutcome()
        if (outcome is InstallOutcome.AwaitingUserAction) launchUserAction(status)

        emit(outcome)

        return outcome is InstallOutcome.AwaitingUserAction
    }

    private fun launchUserAction(status: SessionStatus) {
        val intent = status.userAction ?: return
        launcher.launch(intent)
    }
}
