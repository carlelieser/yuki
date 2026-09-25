package app.yuki.feature.search

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseSortLabelsTest {
    @Test
    fun theEightWebSortOptionsKeepTheirLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(
            listOf(
                "Most stars",
                "Fewest stars",
                "Newest first",
                "Oldest first",
                "Recently updated",
                "Least recently updated",
                "Name A-Z",
                "Name Z-A",
            ),
            BrowseSortOption.entries.map { option -> context.getString(option.label) },
        )
    }
}
