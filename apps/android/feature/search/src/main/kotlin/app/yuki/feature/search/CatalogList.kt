package app.yuki.feature.search

import androidx.annotation.StringRes

enum class CatalogList(
    val wireValue: String,
    val sort: BrowseSortOption,
    @StringRes val title: Int,
) {
    Newest("newest", BrowseSortOption.NewestFirst, R.string.search_catalog_newest_title),
    RecentlyUpdated("updated", BrowseSortOption.RecentlyUpdated, R.string.search_catalog_updated_title),
}

fun readCatalogList(raw: String?): CatalogList? =
    CatalogList.entries.firstOrNull { list -> list.wireValue == raw }
