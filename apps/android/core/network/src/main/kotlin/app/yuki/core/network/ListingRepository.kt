package app.yuki.core.network

import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import javax.inject.Inject
import javax.inject.Singleton

interface ListingRepository {
    suspend fun browse(query: BrowseQuery): Result<ListingPage>

    suspend fun featured(): Result<List<ListingSummary>>

    suspend fun sections(limit: Int): Result<List<CategorySection>>

    suspend fun search(query: String): Result<List<ListingSummary>>

    suspend fun detail(slug: String): Result<ListingDetail>
}

@Singleton
internal class NetworkListingRepository @Inject constructor(
    private val remote: ListingRemoteDataSource,
) : ListingRepository {
    override suspend fun browse(query: BrowseQuery): Result<ListingPage> =
        runRemote("Browse listings (sort=${query.sort}, offset=${query.offset})") {
            remote.browse(query).toDomain()
        }

    override suspend fun featured(): Result<List<ListingSummary>> =
        runRemote("Load featured listings") {
            remote.featured().toDomain().results
        }

    override suspend fun sections(limit: Int): Result<List<CategorySection>> =
        runRemote("Load listing sections (limit=$limit)") {
            remote.sections(limit).toDomain()
        }

    override suspend fun search(query: String): Result<List<ListingSummary>> =
        runRemote("Search listings for q=$query") {
            remote.search(query).results.map(ListingSummaryDto::toDomain)
        }

    override suspend fun detail(slug: String): Result<ListingDetail> =
        runRemote("Load listing detail for slug=$slug") {
            remote.detail(slug).toDomain()
        }
}
