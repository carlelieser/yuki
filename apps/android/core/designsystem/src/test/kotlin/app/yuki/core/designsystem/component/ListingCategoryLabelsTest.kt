package app.yuki.core.designsystem.component

import app.yuki.core.model.ListingCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingCategoryLabelsTest {
    @Test
    fun `every category has its own label`() {
        val labels = ListingCategory.entries.map { category -> category.labelRes() }

        assertEquals(ListingCategory.entries.size, labels.toSet().size)
    }
}
