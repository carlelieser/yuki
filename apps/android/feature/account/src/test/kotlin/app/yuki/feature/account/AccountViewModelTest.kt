package app.yuki.feature.account

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.auth.AuthSession
import app.yuki.core.model.FailureReason
import app.yuki.core.network.AvatarUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private val UPLOAD = AvatarUpload(bytes = byteArrayOf(1, 2, 3), contentType = "image/webp")
private val PICKED = AvatarPick.Ready(UPLOAD)

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val repository = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun signedOut() = FakeSessionStore()

    private fun signedIn() = FakeSessionStore(AuthSession(token = "signed.token", account = ADA))

    @Test
    fun `a signed-out account has nothing to show`() = runTest {
        val viewModel = AccountViewModel(repository, signedOut())

        viewModel.state.test {
            val settled = awaitSettled { state -> !state.isSignedIn }

            assertNull(settled.account)
            assertFalse(settled.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a signed-in account shows the stored profile`() = runTest {
        val viewModel = AccountViewModel(repository, signedIn())

        viewModel.state.test {
            assertEquals(ADA, awaitSettled { state -> state.isSignedIn }.account)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a picked avatar is uploaded and the stored profile is refreshed`() = runTest {
        val store = signedIn()
        val updated = ADA.copy(imageUrl = "/api/users/user-1/avatar?v=2")
        repository.avatarResult = Result.success(updated)
        val viewModel = AccountViewModel(repository, store)

        viewModel.state.test {
            awaitSettled { state -> state.isSignedIn }
            viewModel.onAvatarPicked(PICKED)

            assertEquals(updated, awaitSettled { state -> state.account == updated }.account)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(UPLOAD), repository.uploads)
        assertEquals(updated, store.current?.account)
    }

    @Test
    fun `a failed upload is reported rather than silently dropped`() = runTest {
        repository.avatarResult = Result.failure(TypedFailure(FailureReason.Offline))
        val store = signedIn()
        val viewModel = AccountViewModel(repository, store)

        viewModel.state.test {
            awaitSettled { state -> state.isSignedIn }
            viewModel.onAvatarPicked(PICKED)

            assertEquals(AVATAR_UPLOAD_OFFLINE, awaitSettled { it.message != null }.message)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(ADA, store.current?.account)
    }

    @Test
    fun `a rejected upload repeats the reason the server gave`() = runTest {
        val explanation = "The image must be smaller than 524288 bytes"
        repository.avatarResult =
            Result.failure(TypedFailure(FailureReason.Rejected(explanation)))
        val viewModel = AccountViewModel(repository, signedIn())

        viewModel.state.test {
            awaitSettled { state -> state.isSignedIn }
            viewModel.onAvatarPicked(PICKED)

            assertEquals(explanation, awaitSettled { it.message != null }.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unreadable image is reported without reaching the server`() = runTest {
        val viewModel = AccountViewModel(repository, signedIn())

        viewModel.state.test {
            awaitSettled { state -> state.isSignedIn }
            viewModel.onAvatarPicked(AvatarPick.Unreadable)

            assertEquals(AVATAR_UNREADABLE, awaitSettled { it.message != null }.message)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(emptyList<AvatarUpload>(), repository.uploads)
    }

    @Test
    fun `signing out clears the stored session`() = runTest {
        val store = signedIn()
        val viewModel = AccountViewModel(repository, store)

        viewModel.state.test {
            awaitSettled { state -> state.isSignedIn }
            viewModel.onSignOut()

            awaitSettled { state -> !state.isSignedIn }
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(store.current)
        assertEquals(1, repository.signOutCount)
    }


}

private suspend fun ReceiveTurbine<AccountState>.awaitSettled(
    predicate: (AccountState) -> Boolean,
): AccountState {
    while (true) {
        val state = awaitItem()
        if (predicate(state)) return state
    }
}
