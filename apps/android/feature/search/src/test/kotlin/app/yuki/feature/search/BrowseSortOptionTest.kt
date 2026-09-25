package app.yuki.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseSortOptionTest {
    @Test
    fun `every option round-trips through its wire value`() {
        BrowseSortOption.entries.forEach { option ->
            assertEquals(option, readBrowseSortOption(option.wireValue))
        }
    }

    @Test
    fun `the wire values match the web query parameters`() {
        val wireValues = BrowseSortOption.entries.map(BrowseSortOption::wireValue)

        assertEquals(
            listOf(
                "stars-desc",
                "stars-asc",
                "newest-desc",
                "newest-asc",
                "updated-desc",
                "updated-asc",
                "name-asc",
                "name-desc",
            ),
            wireValues,
        )
    }

    @Test
    fun `the default sort is stars descending`() {
        assertEquals(BrowseSortKey.Stars, BrowseSortOption.Default.key)
        assertEquals(BrowseOrder.Descending, BrowseSortOption.Default.order)
    }

    @Test
    fun `an unknown wire value falls back to the default`() {
        assertEquals(BrowseSortOption.Default, readBrowseSortOption("nonsense"))
        assertEquals(BrowseSortOption.Default, readBrowseSortOption(null))
    }

    @Test
    fun `each sort key and order pair resolves to exactly one option`() {
        val pairs = BrowseSortKey.entries.flatMap { key ->
            BrowseOrder.entries.map { order -> browseSortOptionFor(key, order) }
        }

        assertEquals(BrowseSortOption.entries.size, pairs.toSet().size)
    }
}
