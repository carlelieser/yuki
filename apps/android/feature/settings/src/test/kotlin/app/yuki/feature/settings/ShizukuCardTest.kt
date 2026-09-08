package app.yuki.feature.settings

import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuCardTest {
    @Test
    fun notInstalledOffersTheShizukuWebsite() {
        val card = cardFor(detailOf(ShizukuState.NotInstalled))

        assertEquals(CARD_NOT_INSTALLED_TITLE, card.title)
        assertEquals(ShizukuActionKind.OpenWebsite, card.actionKind)
        assertEquals(ACTION_OPEN_WEBSITE, card.actionLabel)
    }

    @Test
    fun notRunningOffersLaunchingShizuku() {
        val card = cardFor(detailOf(ShizukuState.NotRunning))

        assertEquals(CARD_NOT_RUNNING_TITLE, card.title)
        assertEquals(ShizukuActionKind.LaunchShizuku, card.actionKind)
        assertEquals(ACTION_LAUNCH_SHIZUKU, card.actionLabel)
    }

    @Test
    fun permissionRequiredOffersRequestingPermission() {
        val card = cardFor(detailOf(ShizukuState.PermissionRequired, apiVersion = 13))

        assertEquals(CARD_PERMISSION_TITLE, card.title)
        assertEquals(ShizukuActionKind.RequestPermission, card.actionKind)
        assertEquals(ACTION_REQUEST_PERMISSION, card.actionLabel)
    }

    @Test
    fun readyCarriesNoActionBecauseNothingIsLeftToDo() {
        val card = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, apiVersion = 13))

        assertEquals(CARD_READY_TITLE, card.title)
        assertNull(card.actionKind)
        assertNull(card.actionLabel)
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
            val card = cardFor(detailOf(state))
            assertNotNull("$state must offer an action", card.actionKind)
            assertNotNull("$state must label its action", card.actionLabel)
        }
    }

    @Test
    fun readyNamesTheModeAndApiVersion() {
        val adb = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, apiVersion = 13))
        val root = cardFor(detailOf(ShizukuState.Ready, ShizukuMode.Root, apiVersion = 12))

        assertTrue(adb.description.contains(MODE_ADB))
        assertTrue(adb.description.contains("API 13"))
        assertTrue(root.description.contains(MODE_ROOT))
        assertTrue(root.description.contains("API 12"))
    }

    @Test
    fun anUnknownApiVersionReadsAsUnknownRatherThanMinusOne() {
        assertEquals(API_UNKNOWN, apiVersionLabelFor(UNKNOWN_API_VERSION))
        assertEquals("API 13", apiVersionLabelFor(13))
    }

    @Test
    fun modeIsUnresolvedUntilShizukuIsReady() {
        assertEquals(MODE_UNKNOWN, modeLabelFor(detailOf(ShizukuState.PermissionRequired)))
        assertEquals(
            MODE_ADB,
            modeLabelFor(detailOf(ShizukuState.Ready, ShizukuMode.AdbShell, 13)),
        )
    }
}
