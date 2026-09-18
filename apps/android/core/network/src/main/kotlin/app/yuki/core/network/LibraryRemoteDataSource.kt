package app.yuki.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LibraryRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun library(): LibraryPageDto {
        val response = client.get("api/library")
        return response.decode("Load the library")
    }

    suspend fun record(slug: String, versionTag: String) {
        val response = client.post("api/library") {
            contentType(ContentType.Application.Json)
            setBody(LibraryRecordDto(slug = slug, versionTag = versionTag.ifEmpty { null }))
        }

        if (!response.status.isSuccess()) {
            throw RemoteRequestException(statusFailure(response), "Add slug=$slug to the library")
        }
    }
}
