package app.yuki.feature.listing

import app.yuki.core.model.ListingLinks
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingLinkRowsTest {
    @Test
    fun `lists repository and author for a listing with no optional links`() {
        val listing = detail(license = null).copy(
            links = ListingLinks(
                authorUrl = "https://github.com/nightsky",
                repositoryUrl = "https://github.com/nightsky/aurora",
                homepageUrl = null,
            ),
        )

        assertEquals(listOf("Repository", "Author"), linkRows(listing).map(ListingLinkRow::label))
    }

    @Test
    fun `adds homepage and license rows when both are present`() {
        val labels = linkRows(detail()).map(ListingLinkRow::label)

        assertEquals(listOf("Repository", "Author", "Homepage", "License"), labels)
    }

    @Test
    fun `points the license row at a choosealicense page`() {
        val license = linkRows(detail(license = "Apache License 2.0")).last()

        assertEquals("https://choosealicense.com/licenses/apache-license-2.0/", license.url)
    }

    @Test
    fun `carries the kind that selects each row's icon`() {
        val kinds = linkRows(detail()).map(ListingLinkRow::kind)

        assertEquals(ListingLinkKind.entries, kinds)
    }

    @Test
    fun `renders an unreleased version without a published date`() {
        assertEquals("Unreleased", formatPublished(null))
    }
}
