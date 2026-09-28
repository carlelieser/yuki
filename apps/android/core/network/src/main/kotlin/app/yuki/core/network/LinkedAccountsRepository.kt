package app.yuki.core.network

import app.yuki.core.model.LinkedAccount
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

interface LinkedAccountsRepository {
    suspend fun list(): Result<List<LinkedAccount>>

    suspend fun unlink(accountId: String): Result<Unit>
}

@Serializable
internal data class LinkedAccountDto(
    val id: String,
    val providerId: String,
    val providerUsername: String? = null,
    val providerProfileUrl: String? = null,
)

@Serializable
internal data class UnlinkRequestDto(
    val accountId: String,
)

@Singleton
internal class NetworkLinkedAccountsRepository @Inject constructor(
    private val client: HttpClient,
) : LinkedAccountsRepository {
    override suspend fun list(): Result<List<LinkedAccount>> = runRemote("List linked accounts") {
        val response = client.get("api/auth/list-accounts")
        response.requireSuccess("List linked accounts")

        response.decode<List<LinkedAccountDto>>("List linked accounts").map { it.toDomain() }
    }

    override suspend fun unlink(accountId: String): Result<Unit> =
        runRemote("Unlink account $accountId") {
            val response = client.post("api/auth/unlink-account") {
                contentType(ContentType.Application.Json)
                setBody(UnlinkRequestDto(accountId = accountId))
            }

            response.requireSuccess("Unlink account $accountId")
        }
}

private fun LinkedAccountDto.toDomain(): LinkedAccount = LinkedAccount(
    id = id,
    providerId = providerId,
    username = providerUsername,
    profileUrl = providerProfileUrl,
)
