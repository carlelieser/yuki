package app.yuki.core.network

import app.yuki.core.model.ListingCategory
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

const val SEARCH_RESULT_LIMIT = 24

data class BrowseQuery(
    val sort: String = "stars",
    val order: String = "desc",
    val offset: Int = 0,
    val category: ListingCategory? = null,
)

data class SearchQuery(
    val term: String,
    val sort: String = "stars",
    val order: String = "desc",
    val limit: Int = SEARCH_RESULT_LIMIT,
)

@Singleton
internal class ListingRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun browse(query: BrowseQuery): ListingPageDto {
        val response = client.get("api/listings") {
            parameter("sort", query.sort)
            parameter("order", query.order)
            parameter("offset", query.offset)
            query.category?.let { category -> parameter("category", category.wireValue) }
        }

        return response.decode("Browse listings (sort=${query.sort}, offset=${query.offset})")
    }

    suspend fun featured(): ListingPageDto {
        val response = client.get("api/listings") { parameter("featured", "true") }
        return response.decode("Load featured listings")
    }

    suspend fun sections(limit: Int): CategorySectionsDto {
        val response = client.get("api/feed") { parameter("limit", limit) }
        return response.decode("Load listing sections (limit=$limit)")
    }

    suspend fun search(query: SearchQuery): SearchResultsDto {
        val response = client.get("api/search") {
            parameter("q", query.term)
            parameter("sort", query.sort)
            parameter("order", query.order)
            parameter("limit", query.limit)
        }

        return response.decode("Search listings for q=${query.term}")
    }

    suspend fun detail(slug: String): ListingDetailDto {
        val response = client.get("api/listings/$slug")
        return response.decode("Load listing detail for slug=$slug")
    }

    suspend fun packages(): PackageIndexDto {
        val response = client.get("api/packages")
        return response.decode("Load the package index")
    }
}
