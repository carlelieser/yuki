package app.yuki.core.installer

import android.content.Intent
import android.content.pm.PackageInstaller
import app.yuki.core.model.InstallFailure
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemInstallStrategyTest {
    @Test
    fun `cancelling while waiting for the user abandons the session`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_001)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { true }

        val install = launch { strategy.install(APK, IDENTITY).toList() }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_001, PackageInstaller.STATUS_PENDING_USER_ACTION))
        runCurrent()
        install.cancel()
        runCurrent()

        assertEquals(listOf(9_001), sessions.abandoned)
    }

    @Test
    fun `a finished install leaves its session alone`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_002)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { true }

        val install = launch { strategy.install(APK, IDENTITY).toList() }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_002, PackageInstaller.STATUS_SUCCESS))
        runCurrent()
        install.join()

        assertEquals(emptyList<Int>(), sessions.abandoned)
    }

    @Test
    fun `cancelling after the install finished leaves its session alone`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_003)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { true }

        val install = launch {
            strategy.install(APK, IDENTITY).collect { outcome ->
                if (outcome == InstallOutcome.Succeeded) cancel()
            }
        }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_003, PackageInstaller.STATUS_SUCCESS))
        runCurrent()
        install.join()

        assertEquals(emptyList<Int>(), sessions.abandoned)
    }

    @Test
    fun `an unanswered prompt times out and abandons the session`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_004)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { true }

        val result = async { runCatching { strategy.install(APK, IDENTITY).toList() } }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_004, PackageInstaller.STATUS_PENDING_USER_ACTION))
        advanceTimeBy(CONFIRMATION_TIMEOUT + 1.seconds)

        val failure = result.await().exceptionOrNull() as InstallException
        assertEquals(InstallFailure.ConfirmationTimedOut, failure.failure)
        assertEquals(listOf(9_004), sessions.abandoned)
    }

    @Test
    fun `a prompt answered in time does not time out`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_005)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { true }

        val result = async { strategy.install(APK, IDENTITY).toList() }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_005, PackageInstaller.STATUS_PENDING_USER_ACTION))
        advanceTimeBy(CONFIRMATION_TIMEOUT - 1.seconds)
        InstallStatusBus.publish(statusOf(9_005, PackageInstaller.STATUS_SUCCESS))

        assertEquals(InstallOutcome.Succeeded, result.await().last())
        assertEquals(emptyList<Int>(), sessions.abandoned)
    }

    @Test
    fun `a finished install withdraws its confirmation prompt`() = runTest {
        val prompt = RecordingPrompt()
        val strategy = SystemInstallStrategy(RecordingSessions(sessionId = 9_006), prompt) { true }

        val install = launch { strategy.install(APK, IDENTITY).toList() }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_006, PackageInstaller.STATUS_PENDING_USER_ACTION))
        InstallStatusBus.publish(statusOf(9_006, PackageInstaller.STATUS_SUCCESS))
        runCurrent()
        install.join()

        assertEquals(listOf(9_006), prompt.dismissed)
    }

    @Test
    fun `a cancelled install withdraws its confirmation prompt`() = runTest {
        val prompt = RecordingPrompt()
        val strategy = SystemInstallStrategy(RecordingSessions(sessionId = 9_007), prompt) { true }

        val install = launch { strategy.install(APK, IDENTITY).toList() }
        runCurrent()
        InstallStatusBus.publish(statusOf(9_007, PackageInstaller.STATUS_PENDING_USER_ACTION))
        runCurrent()
        install.cancel()
        runCurrent()

        assertEquals(listOf(9_007), prompt.dismissed)
    }

    @Test
    fun `an app that may not install packages is refused before anything starts`() {
        val sessions = RecordingSessions(sessionId = 9_008)
        val strategy = SystemInstallStrategy(sessions, RecordingPrompt()) { false }

        val error = runCatching { strategy.requireCanInstall() }.exceptionOrNull()

        assertEquals(InstallFailure.InstallPermissionMissing, (error as InstallException).failure)
    }
}

private val APK = File("/downloads/app.apk")
private val IDENTITY = ApkIdentity("com.termux", versionCode = 1L)

private fun statusOf(sessionId: Int, code: Int) =
    SessionStatus(sessionId = sessionId, code = code, message = null, userAction = null)

private class RecordingPrompt : UserActionLauncher {
    val dismissed: MutableList<Int> = mutableListOf()

    override fun launch(sessionId: Int, intent: Intent) = Unit

    override fun dismiss(sessionId: Int) {
        dismissed += sessionId
    }
}

private class RecordingSessions(private val sessionId: Int) : InstallSessions {
    val abandoned: MutableList<Int> = mutableListOf()

    override fun createSession(identity: ApkIdentity): Int = sessionId

    override fun writeApk(sessionId: Int, apk: File) = Unit

    override fun commit(sessionId: Int) = Unit

    override fun abandon(sessionId: Int) {
        abandoned += sessionId
    }
}
