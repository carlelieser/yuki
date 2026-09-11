package app.yuki.feature.search

import app.yuki.core.model.ListingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryToggleTest {
    @Test
    fun selectingAnUnselectedCategoryFiltersByIt() {
        assertEquals(
            ListingCategory.Media,
            toggled(category = ListingCategory.Media, selected = null),
        )
    }

    @Test
    fun selectingTheActiveCategoryClearsTheFilter() {
        assertNull(toggled(category = ListingCategory.Media, selected = ListingCategory.Media))
    }

    @Test
    fun selectingADifferentCategoryReplacesTheFilter() {
        assertEquals(
            ListingCategory.Gaming,
            toggled(category = ListingCategory.Gaming, selected = ListingCategory.Media),
        )
    }
}
