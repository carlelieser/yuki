package app.yuki.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class PagerTransitionTest {
    @Test
    fun theCurrentPageIsFullyPresent() {
        assertEquals(0f, pageTransitionFraction(0f), TOLERANCE)
    }

    @Test
    fun anAdjacentPageIsFullyRecessed() {
        assertEquals(1f, pageTransitionFraction(1f), TOLERANCE)
        assertEquals(1f, pageTransitionFraction(-1f), TOLERANCE)
    }

    @Test
    fun aHalfDraggedPageIsHalfway() {
        assertEquals(0.5f, pageTransitionFraction(0.5f), TOLERANCE)
        assertEquals(0.5f, pageTransitionFraction(-0.5f), TOLERANCE)
    }

    @Test
    fun aDistantPageDoesNotExceedTheLimit() {
        assertEquals(1f, pageTransitionFraction(500f), TOLERANCE)
    }
}

private const val TOLERANCE = 0.0001f
