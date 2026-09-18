package app.yuki.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarInitialsTest {
    @Test
    fun `a full name takes the first letter of the first two words`() {
        assertEquals("AL", initialsOf("Ada Lovelace"))
    }

    @Test
    fun `a longer name stops at two letters`() {
        assertEquals("AA", initialsOf("Ada Augusta King Lovelace"))
    }

    @Test
    fun `a single word takes one letter`() {
        assertEquals("A", initialsOf("Ada"))
    }

    @Test
    fun `initials are uppercased`() {
        assertEquals("AL", initialsOf("ada lovelace"))
    }

    @Test
    fun `extra spacing does not produce empty initials`() {
        assertEquals("AL", initialsOf("  Ada   Lovelace  "))
    }

    @Test
    fun `a signed-out avatar has no initials to show`() {
        assertNull(initialsOf(null))
    }

    @Test
    fun `a blank name has no initials to show`() {
        assertNull(initialsOf("   "))
    }
}
