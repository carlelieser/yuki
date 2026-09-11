package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingTest {
    @Test
    fun `keeps a single decimal place`() {
        assertEquals("4.6", formatRating(4.6))
    }

    @Test
    fun `rounds to the nearest tenth`() {
        assertEquals("4.6", formatRating(4.55))
        assertEquals("1.0", formatRating(1.04))
    }

    @Test
    fun `keeps the tenth on a whole rating`() {
        assertEquals("5.0", formatRating(5.0))
        assertEquals("4.0", formatRating(4.0))
    }
}
