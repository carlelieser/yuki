package app.yuki.feature.settings

import app.yuki.core.shizuku.ShizukuState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuActionHandlerTest {
    private val destinations = RecordingSystemDestinations()
    private var permissionRequests = 0
    private val handler = shizukuActionHandler(destinations) { permissionRequests += 1 }

    @Test
    fun notInstalledOpensTheShizukuWebsite() {
        handler(actionKindFor(ShizukuState.NotInstalled))

        assertEquals(1, destinations.websiteOpens)
        assertEquals(0, destinations.launches)
        assertEquals(0, permissionRequests)
    }

    @Test
    fun notRunningLaunchesShizuku() {
        handler(actionKindFor(ShizukuState.NotRunning))

        assertEquals(1, destinations.launches)
        assertEquals(0, destinations.websiteOpens)
        assertEquals(0, permissionRequests)
    }

    @Test
    fun permissionRequiredRequestsPermission() {
        handler(actionKindFor(ShizukuState.PermissionRequired))

        assertEquals(1, permissionRequests)
        assertEquals(0, destinations.websiteOpens)
        assertEquals(0, destinations.launches)
    }
}

private fun actionKindFor(state: ShizukuState): ShizukuActionKind =
    requireNotNull(cardFor(detailOf(state)).actionKind) { "$state must carry an action" }
