package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StarCountTest {
    @Test
    fun `keeps a count of none as a plain number`() {
        assertEquals("0", formatStarCount(0))
    }

    @Test
    fun `keeps a single star as a plain number`() {
        assertEquals("1", formatStarCount(1))
    }

    @Test
    fun `keeps the largest count below a thousand as a plain number`() {
        assertEquals("999", formatStarCount(999))
    }

    @Test
    fun `drops a trailing zero decimal at a thousand`() {
        assertEquals("1k", formatStarCount(1000))
    }

    @Test
    fun `abbreviates thousands with one decimal`() {
        assertEquals("1.2k", formatStarCount(1200))
    }

    @Test
    fun `abbreviates tens of thousands with one decimal`() {
        assertEquals("15.4k", formatStarCount(15420))
    }

    @Test
    fun `truncates toward zero rather than rounding up to a million`() {
        assertEquals("999.9k", formatStarCount(999999))
    }

    @Test
    fun `drops a trailing zero decimal at a million`() {
        assertEquals("1M", formatStarCount(1000000))
    }

    @Test
    fun `abbreviates millions with one decimal`() {
        assertEquals("1.5M", formatStarCount(1500000))
    }
}
