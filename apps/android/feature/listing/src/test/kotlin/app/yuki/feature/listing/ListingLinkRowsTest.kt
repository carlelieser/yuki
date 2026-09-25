package app.yuki.feature.listing

import app.yuki.core.model.ListingLinks
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
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

        assertEquals(
            listOf(ListingLinkKind.Repository, ListingLinkKind.Author),
            linkRows(listing).map(ListingLinkRow::kind),
        )
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
    fun `formats a published date for the locale`() {
        val published = Instant.parse("2024-01-05T12:00:00Z")

        assertEquals("Jan 5, 2024", formatPublished(published, Locale.US, ZoneOffset.UTC))
        assertEquals("05.01.2024", formatPublished(published, Locale.GERMANY, ZoneOffset.UTC))
    }
}
