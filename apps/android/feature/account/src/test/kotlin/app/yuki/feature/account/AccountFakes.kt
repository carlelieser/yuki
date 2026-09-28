package app.yuki.feature.account

import kotlinx.coroutines.CompletableDeferred
import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.BrowserAuth
import app.yuki.core.auth.BrowserAuthPurpose
import app.yuki.core.auth.BrowserAuthResults
import app.yuki.core.auth.PendingBrowserAuth
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.LinkedAccount
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.AvatarUpload
import app.yuki.core.network.GithubBrowserUrls
import app.yuki.core.network.LinkedAccountsRepository
import app.yuki.core.network.SignedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal val ADA = AuthAccount(
    id = "user-1",
    name = "Ada Lovelace",
    email = "ada@yuki.test",
    imageUrl = null,
)

internal class TypedFailure(override val reason: FailureReason) :
    Exception("failed: $reason"), FailureAware

internal class FakeSessionStore(initial: AuthSession? = null) : SessionStore {
    private val stored = MutableStateFlow(initial)

    val current: AuthSession? get() = stored.value

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

internal class FakeAuthRepository : AuthRepository {
    var signInResult: Result<SignedIn> = Result.success(SignedIn(ADA, token = "signed.token"))
    var signUpResult: Result<SignedIn> = Result.success(SignedIn(ADA, token = null))
    var avatarResult: Result<AuthAccount> = Result.success(ADA)

    var verificationResult: Result<Unit> = Result.success(Unit)
    var ticketExchangeResult: Result<SignedIn> =
        Result.success(SignedIn(ADA, token = "github.token"))
    var ticketResult: Result<String> = Result.success("ticket-1")

    val exchangedTickets = mutableListOf<String>()

    var signOutGate: CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply { complete(Unit) }

    val uploads = mutableListOf<AvatarUpload>()
    val verificationsSentTo = mutableListOf<String>()
    var signOutCount = 0

    override suspend fun signIn(email: String, password: String): Result<SignedIn> = signInResult

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<SignedIn> = signUpResult

    override suspend fun exchangeTicket(ticket: String): Result<SignedIn> {
        exchangedTickets.add(ticket)
        return ticketExchangeResult
    }

    override suspend fun createTicket(): Result<String> = ticketResult

    override suspend fun signOut(): Result<Unit> {
        signOutGate.await()
        signOutCount += 1
        return Result.success(Unit)
    }

    override suspend fun sendVerificationEmail(email: String): Result<Unit> {
        verificationsSentTo.add(email)
        return verificationResult
    }

    override suspend fun session(): Result<AuthAccount?> =
        error("the account screen reads the session from the store")

    override suspend fun updateName(name: String): Result<AuthAccount> =
        error("renaming is not offered by the account screen")

    override suspend fun uploadAvatar(upload: AvatarUpload): Result<AuthAccount> {
        uploads.add(upload)
        return avatarResult
    }
}

internal const val TEST_BASE_URL = "https://yuki.test/"

internal class FakePendingBrowserAuth : PendingBrowserAuth {
    private var pending: Pair<String, BrowserAuthPurpose>? = null
    private var started = 0

    override suspend fun begin(purpose: BrowserAuthPurpose): String {
        started += 1
        return "state-$started".also { state -> pending = state to purpose }
    }

    override suspend fun claim(state: String, purpose: BrowserAuthPurpose): Boolean {
        if (pending != (state to purpose)) return false
        pending = null
        return true
    }
}

internal class BrowserAuthFixture {
    val pending = FakePendingBrowserAuth()
    val results = BrowserAuthResults()
    val browserAuth = BrowserAuth(pending, results)
    val urls = GithubBrowserUrls(TEST_BASE_URL)

    fun returnToApp(query: String) {
        results.deliver("${TEST_BASE_URL}auth/mobile/callback?$query")
    }

    fun signIn(repository: AuthRepository, store: SessionStore) =
        GithubSignIn(browserAuth, repository, store, urls)
}

internal val GITHUB_ACCOUNT = LinkedAccount(
    id = "acc-2",
    providerId = "github",
    username = "ada",
    profileUrl = "https://github.com/ada",
)

internal val PASSWORD_ACCOUNT = LinkedAccount(
    id = "acc-1",
    providerId = "credential",
    username = null,
    profileUrl = null,
)

internal class FakeLinkedAccountsRepository(
    var accounts: List<LinkedAccount> = listOf(PASSWORD_ACCOUNT),
) : LinkedAccountsRepository {
    var unlinkResult: Result<Unit> = Result.success(Unit)
    val unlinked = mutableListOf<String>()

    override suspend fun list(): Result<List<LinkedAccount>> = Result.success(accounts)

    override suspend fun unlink(accountId: String): Result<Unit> {
        unlinked.add(accountId)
        if (unlinkResult.isSuccess) accounts = accounts.filterNot { account -> account.id == accountId }
        return unlinkResult
    }
}

