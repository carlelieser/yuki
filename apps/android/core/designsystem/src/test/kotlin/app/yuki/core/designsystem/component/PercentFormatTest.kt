package app.yuki.core.designsystem.component

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PercentFormatTest {
    @Test
    fun `a fraction reads as a whole percentage`() {
        assertEquals("33%", formatPercent(0.33f, Locale.US))
    }

    @Test
    fun `a download never reads as finished before it is`() {
        assertEquals("99%", formatPercent(0.999f, Locale.US))
    }

    @Test
    fun `the percentage follows the reader's locale`() {
        assertEquals("33 %", formatPercent(0.33f, Locale.GERMANY))
    }
}
