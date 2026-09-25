package app.yuki.feature.explore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturedLoopTest {
    @Test
    fun aSingleListingDoesNotRepeat() {
        assertEquals(1, pageCountFor(1))
        assertEquals(0, startPageFor(1))
    }

    @Test
    fun anEmptyListHasNoPages() {
        assertEquals(0, pageCountFor(0))
    }

    @Test
    fun severalListingsRepeatFarEnoughToScrollEitherWay() {
        val count = 3
        val start = startPageFor(count)

        assertTrue(start > count)
        assertTrue(pageCountFor(count) - start > count)
    }

    @Test
    fun theLoopStartsOnTheFirstListing() {
        val count = 3

        assertEquals(0, startPageFor(count) % count)
    }
}
