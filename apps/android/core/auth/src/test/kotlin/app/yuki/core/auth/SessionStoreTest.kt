package app.yuki.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private val ACCOUNT = AuthAccount(
    id = "user-1",
    name = "Ada Lovelace",
    email = "ada@yuki.test",
    imageUrl = "https://yuki.test/api/users/user-1/avatar?v=1",
)

private val SESSION = AuthSession(token = "signed.token", account = ACCOUNT)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun anUntouchedStoreHasNoSession() = runTest {
        withStore { store -> assertNull(store.read()) }
    }

    @Test
    fun aStoredSessionReadsBackWholeSoTheAccountSurvivesARestart() = runTest {
        withStore { store ->
            store.store(SESSION)

            assertEquals(SESSION, store.read())
        }
    }

    @Test
    fun clearingDropsTheTokenSoRequestsStopCarryingIt() = runTest {
        withStore { store ->
            store.store(SESSION)
            store.clear()

            assertNull(store.read())
        }
    }

    @Test
    fun updatingTheAccountKeepsTheToken() = runTest {
        withStore { store ->
            store.store(SESSION)
            store.updateAccount(ACCOUNT.copy(name = "Ada King", imageUrl = null))

            val stored = store.read()
            assertEquals("signed.token", stored?.token)
            assertEquals("Ada King", stored?.account?.name)
            assertNull(stored?.account?.imageUrl)
        }
    }

    @Test
    fun updatingTheAccountWhileSignedOutDoesNotCreateASession() = runTest {
        withStore { store ->
            store.updateAccount(ACCOUNT)

            assertNull(store.read())
        }
    }

    @Test
    fun theSessionFlowEmitsTheStoredSession() = runTest {
        withStore { store ->
            store.store(SESSION)

            assertEquals(SESSION, store.session.first())
        }
    }

    @Test
    fun aStoredSessionIsStillThereForANewStoreOverTheSameFile() = runTest {
        val file = folder.newFile("round-trip.preferences_pb").also(File::delete)

        withStoreAt(file) { store -> store.store(SESSION) }
        withStoreAt(file) { store -> assertEquals(SESSION, store.read()) }
    }

    private suspend fun withStore(block: suspend (SessionStore) -> Unit) {
        withStoreAt(folder.newFile("session.preferences_pb").also(File::delete), block)
    }

    private suspend fun withStoreAt(file: File, block: suspend (SessionStore) -> Unit) {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val preferences: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope) { file }

        try {
            block(DataStoreSessionStore(preferences))
        } finally {
            scope.cancel()
        }
    }
}
