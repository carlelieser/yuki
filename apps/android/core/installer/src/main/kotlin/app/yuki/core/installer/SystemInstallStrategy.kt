package app.yuki.core.installer

import android.content.Context
import app.yuki.core.model.InstallFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.transformWhile

internal val CONFIRMATION_TIMEOUT: Duration = 10.minutes

internal class SystemInstallStrategy(
    private val sessions: InstallSessions,
    private val launcher: UserActionLauncher,
) : InstallStrategy {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        sessions = InstallSessionWriter(context),
        launcher = UserActionPrompt(context),
    )

    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        val session = PendingSession(sessions.createSession(identity))
        sessions.writeApk(session.id, apk)

        try {
            emitAll(outcomesOf(session))
        } catch (error: TimeoutCancellationException) {
            sessions.abandon(session.id)
            throw InstallException(
                InstallFailure.ConfirmationTimedOut,
                "Install session ${session.id} was not confirmed within $CONFIRMATION_TIMEOUT",
                error,
            )
        } catch (error: CancellationException) {
            if (!session.isSettled) sessions.abandon(session.id)
            throw error
        } finally {
            launcher.dismiss(session.id)
        }
    }

    @OptIn(FlowPreview::class)
    private fun outcomesOf(session: PendingSession): Flow<InstallOutcome> =
        InstallStatusBus.updates
            .onSubscription { sessions.commit(session.id) }
            .filter { status -> status.sessionId == session.id }
            .timeout(CONFIRMATION_TIMEOUT)
            .map { status -> outcomeOf(session, status) }
            .transformWhile { outcome ->
                emit(outcome)
                outcome is InstallOutcome.AwaitingUserAction
            }

    private fun outcomeOf(session: PendingSession, status: SessionStatus): InstallOutcome {
        val outcome = status.toOutcome()

        if (outcome is InstallOutcome.AwaitingUserAction) {
            status.userAction?.let { intent -> launcher.launch(session.id, intent) }
        } else {
            session.isSettled = true
        }

        return outcome
    }
}

private class PendingSession(val id: Int) {
    var isSettled: Boolean = false
}
