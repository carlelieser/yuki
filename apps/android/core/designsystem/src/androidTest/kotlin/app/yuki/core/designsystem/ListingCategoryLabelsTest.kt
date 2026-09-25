package app.yuki.core.designsystem

import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.component.labelRes
import app.yuki.core.model.ListingCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingCategoryLabelsTest {
    @Test
    fun labelsMatchTheWebApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(
            listOf(
                "System",
                "Apps",
                "Files",
                "Media",
                "Gaming",
                "Automation",
                "Network",
                "Privacy",
                "Developer",
                "Device",
                "Customization",
                "Connectivity",
                "Utilities",
            ),
            ListingCategory.entries.map { category -> context.getString(category.labelRes()) },
        )
    }
}
