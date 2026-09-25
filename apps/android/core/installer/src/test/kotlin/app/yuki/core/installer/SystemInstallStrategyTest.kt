package app.yuki.core.installer

import android.content.pm.PackageInstaller
import java.io.File
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemInstallStrategyTest {
    @Test
    fun `cancelling while waiting for the user abandons the session`() = runTest {
        val sessions = RecordingSessions(sessionId = 9_001)
        val strategy = SystemInstallStrategy(sessions) {}

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
        val strategy = SystemInstallStrategy(sessions) {}

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
        val strategy = SystemInstallStrategy(sessions) {}

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
}

private val APK = File("/downloads/app.apk")
private val IDENTITY = ApkIdentity("com.termux", versionCode = 1L)

private fun statusOf(sessionId: Int, code: Int) =
    SessionStatus(sessionId = sessionId, code = code, message = null, userAction = null)

private class RecordingSessions(private val sessionId: Int) : InstallSessions {
    val abandoned: MutableList<Int> = mutableListOf()

    override fun createSession(identity: ApkIdentity): Int = sessionId

    override fun writeApk(sessionId: Int, apk: File) = Unit

    override fun commit(sessionId: Int) = Unit

    override fun abandon(sessionId: Int) {
        abandoned += sessionId
    }
}
