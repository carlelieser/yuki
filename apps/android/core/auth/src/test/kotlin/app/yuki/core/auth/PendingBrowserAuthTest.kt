package app.yuki.core.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val ELEVEN_MINUTES = 11 * 60 * 1000L

@OptIn(ExperimentalCoroutinesApi::class)
class PendingBrowserAuthTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private var now = 0L

    @Test
    fun `the state that was started can be claimed once`() = runTest {
        withPending { pending ->
            val state = pending.begin(BrowserAuthPurpose.SignIn)

            assertTrue(pending.claim(state, BrowserAuthPurpose.SignIn))
            assertFalse(pending.claim(state, BrowserAuthPurpose.SignIn))
        }
    }

    @Test
    fun `a state that was never started is rejected`() = runTest {
        withPending { pending ->
            pending.begin(BrowserAuthPurpose.SignIn)

            assertFalse(pending.claim("forged", BrowserAuthPurpose.SignIn))
        }
    }

    @Test
    fun `a sign-in state cannot complete a link`() = runTest {
        withPending { pending ->
            val state = pending.begin(BrowserAuthPurpose.SignIn)

            assertFalse(pending.claim(state, BrowserAuthPurpose.Link))
            assertTrue(pending.claim(state, BrowserAuthPurpose.SignIn))
        }
    }

    @Test
    fun `an abandoned state expires`() = runTest {
        withPending { pending ->
            val state = pending.begin(BrowserAuthPurpose.SignIn)
            now += ELEVEN_MINUTES

            assertFalse(pending.claim(state, BrowserAuthPurpose.SignIn))
        }
    }

    @Test
    fun `each attempt gets a new state`() = runTest {
        withPending { pending ->
            assertNotEquals(
                pending.begin(BrowserAuthPurpose.SignIn),
                pending.begin(BrowserAuthPurpose.SignIn),
            )
        }
    }

    private suspend fun withPending(block: suspend (PendingBrowserAuth) -> Unit) {
        val file = folder.newFile("browser_auth.preferences_pb").also(File::delete)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val preferences = PreferenceDataStoreFactory.create(scope = scope) { file }

        try {
            block(DataStorePendingBrowserAuth(preferences) { now })
        } finally {
            scope.cancel()
        }
    }
}
