package app.yuki.feature.search

import androidx.annotation.StringRes

enum class BrowseSortKey(val wireValue: String) {
    Stars("stars"),
    Newest("newest"),
    Updated("updated"),
    Name("name"),
}

enum class BrowseOrder(val wireValue: String) {
    Ascending("asc"),
    Descending("desc"),
}

enum class BrowseSortOption(
    val key: BrowseSortKey,
    val order: BrowseOrder,
    @StringRes val label: Int,
) {
    MostStars(BrowseSortKey.Stars, BrowseOrder.Descending, R.string.search_sort_most_stars),
    FewestStars(BrowseSortKey.Stars, BrowseOrder.Ascending, R.string.search_sort_fewest_stars),
    NewestFirst(BrowseSortKey.Newest, BrowseOrder.Descending, R.string.search_sort_newest_first),
    OldestFirst(BrowseSortKey.Newest, BrowseOrder.Ascending, R.string.search_sort_oldest_first),
    RecentlyUpdated(BrowseSortKey.Updated, BrowseOrder.Descending, R.string.search_sort_recently_updated),
    LeastRecentlyUpdated(BrowseSortKey.Updated, BrowseOrder.Ascending, R.string.search_sort_least_recently_updated),
    NameAscending(BrowseSortKey.Name, BrowseOrder.Ascending, R.string.search_sort_name_ascending),
    NameDescending(BrowseSortKey.Name, BrowseOrder.Descending, R.string.search_sort_name_descending);

    val wireValue: String get() = "${key.wireValue}-${order.wireValue}"

    companion object {
        val Default: BrowseSortOption = MostStars
    }
}

fun readBrowseSortOption(raw: String?): BrowseSortOption =
    BrowseSortOption.entries.firstOrNull { option -> option.wireValue == raw }
        ?: BrowseSortOption.Default

fun browseSortOptionFor(key: BrowseSortKey, order: BrowseOrder): BrowseSortOption =
    BrowseSortOption.entries.first { option -> option.key == key && option.order == order }
