package app.yuki.core.designsystem.component

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingFormatTest {
    @Test
    fun `a rating keeps one decimal`() {
        assertEquals("4.6", formatRating(4.6, Locale.US))
        assertEquals("5.0", formatRating(5.0, Locale.US))
        assertEquals("4.0", formatRating(4.0, Locale.US))
    }

    @Test
    fun `a rating rounds half up to the nearest tenth`() {
        assertEquals("4.6", formatRating(4.55, Locale.US))
        assertEquals("1.0", formatRating(1.04, Locale.US))
    }

    @Test
    fun `a rating uses the locale decimal separator`() {
        assertEquals("4,6", formatRating(4.6, Locale.GERMANY))
    }
}
