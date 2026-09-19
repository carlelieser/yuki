package app.yuki.core.network

import app.yuki.core.model.FailureReason
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

internal const val AUTH_TOKEN_HEADER = "set-auth-token"

private const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
private const val USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS"
private const val EMPTY_BODY = "{}"
private const val AVATAR_FIELD = "avatar"

internal data class SignedInResponse(
    val account: AccountDto,
    val token: String?,
)

@Singleton
internal class AuthRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun signIn(email: String, password: String): SignedInResponse {
        val response = client.post("api/auth/sign-in/email") {
            contentType(ContentType.Application.Json)
            setBody(SignInRequestDto(email = email, password = password))
        }

        return response.toSignedIn("Sign in as $email")
    }

    suspend fun signUp(name: String, email: String, password: String): SignedInResponse {
        val response = client.post("api/auth/sign-up/email") {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequestDto(name = name, email = email, password = password))
        }

        return response.toSignedIn("Sign up as $email")
    }

    suspend fun sendVerificationEmail(email: String) {
        val response = client.post("api/auth/send-verification-email") {
            contentType(ContentType.Application.Json)
            setBody(SendVerificationRequestDto(email = email))
        }

        response.requireSuccess("Resend the verification email to $email")
    }

    suspend fun signOut() {
        val response = client.post("api/auth/sign-out") {
            contentType(ContentType.Application.Json)
            setBody(EMPTY_BODY)
        }

        response.requireSuccess("Sign out")
    }

    suspend fun session(): AccountDto? {
        val response = client.get("api/auth/get-session")
        if (response.status == HttpStatusCode.Unauthorized) return null

        return response.decode<SessionResponseDto>("Load the session").user
    }

    suspend fun updateName(name: String): AccountDto {
        val response = client.post("api/auth/update-user") {
            contentType(ContentType.Application.Json)
            setBody(UpdateAccountRequestDto(name = name))
        }

        return response.reloadAccount("Rename the account to $name")
    }

    suspend fun uploadAvatar(bytes: ByteArray, contentType: String): AccountDto {
        val response = client.post("api/account/avatar") {
            setBody(avatarForm(bytes, contentType))
        }

        return response.reloadAccount("Upload a profile picture")
    }

    private suspend fun HttpResponse.reloadAccount(operation: String): AccountDto {
        requireSuccess(operation)

        return session() ?: throw RemoteRequestException(FailureReason.Unauthorized, operation)
    }
}

private fun avatarForm(bytes: ByteArray, contentType: String) = MultiPartFormDataContent(
    formData {
        append(
            key = AVATAR_FIELD,
            value = bytes,
            headers = Headers.build {
                append(HttpHeaders.ContentType, contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"$AVATAR_FIELD\"")
            },
        )
    },
)

private suspend fun HttpResponse.toSignedIn(operation: String): SignedInResponse {
    requireSuccess(operation)

    val account = decode<SignInResponseDto>(operation).user
        ?: throw RemoteRequestException(
            FailureReason.Unexpected(
                IllegalStateException("$operation returned no account"),
            ),
            operation,
        )

    return SignedInResponse(account = account, token = headers[AUTH_TOKEN_HEADER])
}

private suspend fun HttpResponse.requireSuccess(operation: String) {
    if (status.isSuccess()) return

    throw RemoteRequestException(authFailure(), operation)
}

private suspend fun HttpResponse.authFailure(): FailureReason =
    when (runCatching { body<AuthErrorDto>() }.getOrNull()?.code) {
        EMAIL_NOT_VERIFIED -> FailureReason.EmailNotVerified
        USER_ALREADY_EXISTS -> FailureReason.AccountExists
        else -> statusFailure(this)
    }
