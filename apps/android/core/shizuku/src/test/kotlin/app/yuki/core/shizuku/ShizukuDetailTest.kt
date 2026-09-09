package app.yuki.core.shizuku

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuDetailTest {
    private val gateway = FakeShizukuGateway()
    private val monitor = ShizukuMonitor(gateway)

    @Test
    fun `resolves adb shell mode from uid 2000`() {
        gateway.shizukuUid = ADB_SHELL_UID

        monitor.start()

        assertEquals(ShizukuMode.AdbShell, monitor.detail.value.mode)
    }

    @Test
    fun `resolves root mode from uid 0`() {
        gateway.shizukuUid = ROOT_UID

        monitor.start()

        assertEquals(ShizukuMode.Root, monitor.detail.value.mode)
    }

    @Test
    fun `resolves an unrecognised uid as an unknown mode`() {
        gateway.shizukuUid = 1234

        monitor.start()

        assertEquals(ShizukuMode.Unknown, monitor.detail.value.mode)
    }

    @Test
    fun `exposes the shizuku api version once the binder answers`() {
        gateway.apiVersion = 13

        monitor.start()

        assertEquals(13, monitor.detail.value.apiVersion)
    }

    @Test
    fun `reports an unknown api version while shizuku is not running`() {
        gateway.running = false

        monitor.start()

        assertEquals(UNKNOWN_API_VERSION, monitor.detail.value.apiVersion)
    }

    @Test
    fun `does not claim a mode before permission is granted`() {
        gateway.permission = DENIED
        gateway.shizukuUid = ROOT_UID

        monitor.start()

        assertEquals(ShizukuMode.Unknown, monitor.detail.value.mode)
    }
}
