package app.yuki.core.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FIRST = "https://cdn.test/one.png"
private const val SECOND = "https://cdn.test/two.png"

class ScreenshotFocusTest {
    @Test
    fun everyScreenshotMatchesBeforeOneIsFocused() {
        val focus = ScreenshotFocus()

        assertTrue(focus.isFocused(FIRST))
        assertTrue(focus.isFocused(SECOND))
    }

    @Test
    fun onlyTheFocusedScreenshotMatches() {
        val focus = ScreenshotFocus()

        focus.focus(SECOND)

        assertFalse(focus.isFocused(FIRST))
        assertTrue(focus.isFocused(SECOND))
    }

    @Test
    fun aFocusFromAnotherListingIsDropped() {
        val focus = ScreenshotFocus()
        focus.focus(SECOND)

        focus.confine(listOf("https://cdn.test/other.png"))

        assertNull(focus.url)
    }

    @Test
    fun aFocusWithinTheListingSurvives() {
        val focus = ScreenshotFocus()
        focus.focus(SECOND)

        focus.confine(listOf(FIRST, SECOND))

        assertTrue(focus.isFocused(SECOND))
        assertFalse(focus.isFocused(FIRST))
    }
}
