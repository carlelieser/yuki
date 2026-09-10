package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListingCategoryTest {
    @Test
    fun `every wire value round-trips back to its category`() {
        ListingCategory.entries.forEach { category ->
            assertEquals(category, readListingCategory(category.wireValue))
        }
    }

    @Test
    fun `carries the wire values the server sends in schema order`() {
        assertEquals(
            listOf(
                "system_tweaks",
                "app_management",
                "file_management",
                "media",
                "gaming",
                "automation",
                "networking",
                "privacy_security",
                "developer_tools",
                "device_specific",
                "customization",
                "connectivity",
                "utilities",
            ),
            ListingCategory.entries.map { category -> category.wireValue },
        )
    }

    @Test
    fun `carries the labels the web app displays`() {
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
            ListingCategory.entries.map { category -> category.label },
        )
    }

    @Test
    fun `an unknown category is null rather than a crash`() {
        assertNull(readListingCategory("teleportation"))
        assertNull(readListingCategory(""))
        assertNull(readListingCategory("SYSTEM_TWEAKS"))
    }

    @Test
    fun `an absent category is null`() {
        assertNull(readListingCategory(null))
    }
}
