package app.yuki.feature.settings

import androidx.compose.runtime.Composable
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsGroup
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeContributor(
    override val group: SettingsGroup,
    override val order: Int = 0,
) : SettingsContributor {
    @Composable
    override fun Content() = Unit
}

class SettingsContributorOrderTest {
    @Test
    fun `contributors are ordered by group`() {
        val legal = FakeContributor(SettingsGroup.Legal)
        val account = FakeContributor(SettingsGroup.Account)
        val system = FakeContributor(SettingsGroup.System)
        val library = FakeContributor(SettingsGroup.Library)

        val ordered = setOf(legal, system, library, account).ordered()

        assertEquals(listOf(account, library, system, legal), ordered)
    }

    @Test
    fun `contributors in one group keep their declared order`() {
        val first = FakeContributor(SettingsGroup.System, order = 1)
        val second = FakeContributor(SettingsGroup.System, order = 2)

        val ordered = setOf(second, first).ordered()

        assertEquals(listOf(first, second), ordered)
    }

    @Test
    fun `legal contributors are separated into the footer`() {
        val account = FakeContributor(SettingsGroup.Account)
        val legal = FakeContributor(SettingsGroup.Legal)

        val sections = listOf(account, legal).partitionFooter()

        assertEquals(listOf(account), sections.body)
        assertEquals(listOf(legal), sections.footer)
    }
}
