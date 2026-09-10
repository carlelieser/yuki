package app.yuki.feature.search

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
    val label: String,
) {
    MostStars(BrowseSortKey.Stars, BrowseOrder.Descending, "Most stars"),
    FewestStars(BrowseSortKey.Stars, BrowseOrder.Ascending, "Fewest stars"),
    NewestFirst(BrowseSortKey.Newest, BrowseOrder.Descending, "Newest first"),
    OldestFirst(BrowseSortKey.Newest, BrowseOrder.Ascending, "Oldest first"),
    RecentlyUpdated(BrowseSortKey.Updated, BrowseOrder.Descending, "Recently updated"),
    LeastRecentlyUpdated(BrowseSortKey.Updated, BrowseOrder.Ascending, "Least recently updated"),
    NameAscending(BrowseSortKey.Name, BrowseOrder.Ascending, "Name A-Z"),
    NameDescending(BrowseSortKey.Name, BrowseOrder.Descending, "Name Z-A");

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
