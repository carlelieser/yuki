package app.yuki.feature.library

import app.yuki.core.auth.AuthSession
import app.yuki.core.model.AuthAccount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun session(id: String) = AuthSession(
    token = "token-$id",
    account = AuthAccount(id = id, name = "User $id", email = "$id@yuki.test", imageUrl = null),
)

@OptIn(ExperimentalCoroutinesApi::class)
class SignInLibrarySyncTest {
    private val remote = FakeRemoteLibrary()
    private val installs = FakeInstallStore(listOf(detectedApp(OBTAINIUM)))

    private fun TestScope.following(sessions: FakeSessionStore) {
        val sync = LibrarySync(detection = FakeDetection(), store = installs, remote = remote)

        backgroundScope.launch { SignInLibrarySync(sessions, sync).follow() }
        runCurrent()
    }

    @Test
    fun `signing in uploads installs the server is missing`() = runTest {
        val sessions = FakeSessionStore()
        following(sessions)

        sessions.store(session("ada"))
        runCurrent()

        assertEquals(listOf(OBTAINIUM.slug), remote.recorded)
    }

    @Test
    fun `an account already signed in at launch is not synced again`() = runTest {
        following(FakeSessionStore(session("ada")))

        assertEquals(emptyList<String>(), remote.recorded)
    }

    @Test
    fun `refreshing the signed-in profile does not sync`() = runTest {
        val sessions = FakeSessionStore()
        following(sessions)
        sessions.store(session("ada"))
        runCurrent()

        sessions.updateAccount(session("ada").account.copy(name = "Ada L."))
        runCurrent()

        assertEquals(1, remote.recorded.size)
    }

    @Test
    fun `signing out and back in syncs again`() = runTest {
        val sessions = FakeSessionStore(session("ada"))
        following(sessions)

        sessions.clear()
        runCurrent()
        sessions.store(session("ada"))
        runCurrent()

        assertEquals(listOf(OBTAINIUM.slug), remote.recorded)
    }
}

private class FakeDetection : DetectedInstallRefresh {
    override suspend fun reconcile() = Unit
}
