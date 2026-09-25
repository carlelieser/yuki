package app.yuki.feature.settings

import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ShizukuCardTest {
    @Test
    fun notInstalledOffersTheShizukuWebsite() {
        val card = cardFor(detailOf(ShizukuState.NotInstalled))

        assertEquals(R.string.settings_card_not_installed_title, card.title)
        assertEquals(ShizukuActionKind.OpenWebsite, card.actionKind)
    }

    @Test
    fun notRunningOffersLaunchingShizuku() {
        val card = cardFor(detailOf(ShizukuState.NotRunning))

        assertEquals(R.string.settings_card_not_running_title, card.title)
        assertEquals(ShizukuActionKind.LaunchShizuku, card.actionKind)
    }

    @Test
    fun permissionRequiredOffersRequestingPermission() {
        val card = cardFor(detailOf(ShizukuState.PermissionRequired, apiVersion = 13))

        assertEquals(R.string.settings_card_permission_title, card.title)
        assertEquals(ShizukuActionKind.RequestPermission, card.actionKind)
    }

    @Test
    fun readyCarriesNoActionBecauseNothingIsLeftToDo() {
        val card = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, apiVersion = 13))

        assertEquals(R.string.settings_card_ready_title, card.title)
        assertNull(card.actionKind)
        assertEquals(StatusTone.Positive, card.tone)
    }

    @Test
    fun everyUnreadyStateCarriesExactlyOneAction() {
        val unready = listOf(
            ShizukuState.NotInstalled,
            ShizukuState.NotRunning,
            ShizukuState.PermissionRequired,
        )

        unready.forEach { state ->
            assertNotNull("$state must offer an action", cardFor(detailOf(state)).actionKind)
        }
    }

    @Test
    fun readyNamesTheModeAndApiVersion() {
        val adb = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, apiVersion = 13))
        val root = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.Root, apiVersion = 12))

        assertEquals(
            ShizukuCardDescription.Ready(ShizukuMode.AdbShell, ShizukuApiVersion.Known(13)),
            adb.description,
        )
        assertEquals(
            ShizukuCardDescription.Ready(ShizukuMode.Root, ShizukuApiVersion.Known(12)),
            root.description,
        )
    }

    @Test
    fun anUnknownApiVersionReadsAsUnknownRatherThanMinusOne() {
        assertEquals(ShizukuApiVersion.Unknown, apiVersionOf(UNKNOWN_API_VERSION))
        assertEquals(ShizukuApiVersion.Known(13), apiVersionOf(13))
    }

    @Test
    fun modeIsUnresolvedUntilShizukuIsReady() {
        assertEquals(
            R.string.settings_shizuku_mode_unknown,
            modeLabelFor(detailOf(ShizukuState.PermissionRequired).mode),
        )
        assertEquals(
            R.string.settings_shizuku_mode_adb,
            modeLabelFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, 13).mode),
        )
    }
}
