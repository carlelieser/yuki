package app.yuki.core.designsystem

import app.yuki.core.designsystem.component.formatStarCount
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class StarCountFormatTest {
    @Test
    fun smallCountsAreShownInFull() {
        assertEquals("0", formatStarCount(0, Locale.US))
        assertEquals("999", formatStarCount(999, Locale.US))
    }

    @Test
    fun thousandsAreAbbreviatedToOneDecimal() {
        assertEquals("1K", formatStarCount(1_000, Locale.US))
        assertEquals("1.2K", formatStarCount(1_200, Locale.US))
        assertEquals("15.4K", formatStarCount(15_420, Locale.US))
    }

    @Test
    fun abbreviationsTruncateRatherThanRoundUp() {
        assertEquals("999.9K", formatStarCount(999_999, Locale.US))
    }

    @Test
    fun millionsAreAbbreviated() {
        assertEquals("1M", formatStarCount(1_000_000, Locale.US))
        assertEquals("1.5M", formatStarCount(1_500_000, Locale.US))
    }
}
