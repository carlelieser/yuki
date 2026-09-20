package app.yuki.feature.library

import app.cash.turbine.test
import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val ADA = AuthAccount(
    id = "user-1",
    name = "Ada Lovelace",
    email = "ada@yuki.test",
    imageUrl = null,
)

private class FakeSessionStore(initial: AuthSession? = null) : SessionStore {
    private val stored = MutableStateFlow(initial)

    override val session: Flow<AuthSession?> = stored.asStateFlow()

    override suspend fun read(): AuthSession? = stored.value

    override suspend fun store(session: AuthSession) {
        stored.value = session
    }

    override suspend fun updateAccount(account: AuthAccount) {
        stored.value = stored.value?.copy(account = account)
    }

    override suspend fun clear() {
        stored.value = null
    }
}

private class RecordingRefresh : DetectedInstallRefresh {
    var count = 0

    override suspend fun reconcile() {
        count += 1
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySyncViewModelTest {
    private val refresh = RecordingRefresh()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelWith(session: AuthSession?) =
        LibrarySyncViewModel(LibrarySync(refresh), FakeSessionStore(session))

    @Test
    fun `a signed-out visitor is not offered library actions`() = runTest {
        val viewModel = viewModelWith(null)

        viewModel.isSignedIn.test {
            awaitItem()
            runCurrent()

            assertFalse(viewModel.isSignedIn.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a signed-in account is offered library actions`() = runTest {
        val viewModel = viewModelWith(AuthSession(token = "signed.token", account = ADA))

        viewModel.isSignedIn.test {
            awaitItem()
            runCurrent()

            assertTrue(viewModel.isSignedIn.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncing reconciles detected installs`() = runTest {
        val viewModel = viewModelWith(AuthSession(token = "signed.token", account = ADA))

        viewModel.onSync()
        runCurrent()

        assertEquals(1, refresh.count)
    }
}
