package app.yuki.core.network

import app.yuki.core.model.AuthAccount
import app.yuki.core.model.FailureReason
import app.yuki.core.model.failureReason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ACCOUNT_BODY = """
{"user":{"id":"user-1","name":"Ada Lovelace","email":"ada@yuki.test","image":"/avatar.png"}}
"""

private val ADA = AuthAccount(
    id = "user-1",
    name = "Ada Lovelace",
    email = "ada@yuki.test",
    imageUrl = "/avatar.png",
)

class AuthRepositoryTest {
    @Test
    fun `signing in returns the account and the token the server issued`() = runTest {
        val repository = repository(
            body = ACCOUNT_BODY,
            responseHeaders = headersOf(AUTH_TOKEN_HEADER, "signed.token"),
        )

        val signedIn = repository.signIn("ada@yuki.test", "hunter2000").getOrThrow()

        assertEquals(ADA, signedIn.account)
        assertEquals("signed.token", signedIn.token)
    }

    @Test
    fun `signing up returns no token because the account is unverified`() = runTest {
        val signedIn = repository(ACCOUNT_BODY)
            .signUp("Ada Lovelace", "ada@yuki.test", "hunter2000")
            .getOrThrow()

        assertEquals(ADA, signedIn.account)
        assertNull(signedIn.token)
    }

    @Test
    fun `an unverified sign-in is told apart from a wrong password`() = runTest {
        val repository = repository(
            body = """{"code":"EMAIL_NOT_VERIFIED"}""",
            status = HttpStatusCode.Forbidden,
        )

        val failure = repository.signIn("ada@yuki.test", "hunter2000").exceptionOrNull()!!

        assertEquals(FailureReason.EmailNotVerified, failure.failureReason())
    }

    @Test
    fun `a wrong password reads as unauthorized`() = runTest {
        val repository = repository(
            body = """{"code":"INVALID"}""",
            status = HttpStatusCode.Unauthorized,
        )

        val failure = repository.signIn("ada@yuki.test", "wrong").exceptionOrNull()!!

        assertEquals(FailureReason.Unauthorized, failure.failureReason())
    }

    @Test
    fun `a taken email reads as an existing account`() = runTest {
        val repository = repository(
            body = """{"code":"USER_ALREADY_EXISTS"}""",
            status = HttpStatusCode.UnprocessableEntity,
        )

        val failure = repository.signUp("Ada", "ada@yuki.test", "hunter2000").exceptionOrNull()!!

        assertEquals(FailureReason.AccountExists, failure.failureReason())
    }

    @Test
    fun `a rejected upload keeps the explanation the server wrote`() = runTest {
        val repository = repository(
            body = """{"message":"Unsupported image type \"image/heic\""}""",
            status = HttpStatusCode.BadRequest,
        )

        val upload = AvatarUpload(bytes = byteArrayOf(1, 2, 3), contentType = "image/heic")
        val failure = repository.uploadAvatar(upload).exceptionOrNull()!!

        assertEquals(
            FailureReason.Rejected("""Unsupported image type "image/heic""""),
            failure.failureReason(),
        )
    }

    @Test
    fun `a server error keeps its status rather than quoting the server's prose`() = runTest {
        val repository = repository(
            body = """{"message":"Internal Error"}""",
            status = HttpStatusCode.InternalServerError,
        )

        val upload = AvatarUpload(bytes = byteArrayOf(1, 2, 3), contentType = "image/webp")
        val failure = repository.uploadAvatar(upload).exceptionOrNull()!!

        assertEquals(FailureReason.Server(500), failure.failureReason())
    }

    @Test
    fun `a signed-out session reads as no account rather than a failure`() = runTest {
        val repository = repository(body = "", status = HttpStatusCode.Unauthorized)

        assertNull(repository.session().getOrThrow())
    }

    @Test
    fun `a failure names the operation and the email`() = runTest {
        val repository = repository(body = "{}", status = HttpStatusCode.InternalServerError)

        val failure = repository.signIn("ada@yuki.test", "hunter2000").exceptionOrNull()!!

        assertTrue(failure.message!!.contains("ada@yuki.test"))
    }

    private fun repository(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        responseHeaders: Headers = headersOf(),
    ): AuthRepository {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = Headers.build {
                    appendAll(responseHeaders)
                    append("Content-Type", ContentType.Application.Json.toString())
                },
            )
        }

        return NetworkAuthRepository(AuthRemoteDataSource(YukiHttpClient.create(BASE_URL, engine)))
    }
}
