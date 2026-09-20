package app.yuki.feature.account

import kotlinx.coroutines.CompletableDeferred
import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.AvatarUpload
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
