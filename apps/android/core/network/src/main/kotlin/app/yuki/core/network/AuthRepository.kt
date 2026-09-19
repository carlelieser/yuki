package app.yuki.core.network

import app.yuki.core.model.AuthAccount
import javax.inject.Inject
import javax.inject.Singleton

data class SignedIn(
    val account: AuthAccount,
    val token: String?,
)

data class AvatarUpload(
    val bytes: ByteArray,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean =
        other is AvatarUpload && contentType == other.contentType && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + contentType.hashCode()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<SignedIn>

    suspend fun signUp(name: String, email: String, password: String): Result<SignedIn>

    suspend fun signOut(): Result<Unit>

    suspend fun session(): Result<AuthAccount?>

    suspend fun updateName(name: String): Result<AuthAccount>

    suspend fun uploadAvatar(upload: AvatarUpload): Result<AuthAccount>
}

@Singleton
internal class NetworkAuthRepository @Inject constructor(
    private val remote: AuthRemoteDataSource,
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<SignedIn> =
        runRemote("Sign in as $email") { remote.signIn(email, password).toDomain() }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<SignedIn> =
        runRemote("Sign up as $email") { remote.signUp(name, email, password).toDomain() }

    override suspend fun signOut(): Result<Unit> = runRemote("Sign out") { remote.signOut() }

    override suspend fun session(): Result<AuthAccount?> =
        runRemote("Load the session") { remote.session()?.toDomain() }

    override suspend fun updateName(name: String): Result<AuthAccount> =
        runRemote("Rename the account to $name") { remote.updateName(name).toDomain() }

    override suspend fun uploadAvatar(upload: AvatarUpload): Result<AuthAccount> =
        runRemote("Upload a profile picture") {
            remote.uploadAvatar(upload.bytes, upload.contentType).toDomain()
        }
}

private fun SignedInResponse.toDomain(): SignedIn = SignedIn(
    account = account.toDomain(),
    token = token,
)
