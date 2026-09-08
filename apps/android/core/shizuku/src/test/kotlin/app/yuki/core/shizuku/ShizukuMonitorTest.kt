package app.yuki.core.shizuku

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuMonitorTest {
    private val gateway = FakeShizukuGateway()
    private val monitor = ShizukuMonitor(gateway)

    @Test
    fun `reports not installed when shizuku is absent`() {
        gateway.installed = false

        monitor.start()

        assertEquals(ShizukuState.NotInstalled, monitor.state)
    }

    @Test
    fun `reports not running when installed but the binder does not answer`() {
        gateway.running = false

        monitor.start()

        assertEquals(ShizukuState.NotRunning, monitor.state)
    }

    @Test
    fun `reports permission required when running but not yet authorised`() {
        gateway.permission = DENIED

        monitor.start()

        assertEquals(ShizukuState.PermissionRequired, monitor.state)
    }

    @Test
    fun `reports ready when running and authorised`() {
        monitor.start()

        assertEquals(ShizukuState.Ready, monitor.state)
    }

    @Test
    fun `treats a pre v11 release as not installed`() {
        gateway.preV11 = true

        monitor.start()

        assertEquals(ShizukuState.NotInstalled, monitor.state)
    }

    @Test
    fun `moves to ready when the binder arrives while stopped`() {
        gateway.running = false
        monitor.start()
        assertEquals(ShizukuState.NotRunning, monitor.state)

        gateway.running = true
        gateway.emitBinderReceived()

        assertEquals(ShizukuState.Ready, monitor.state)
    }

    @Test
    fun `falls back to not running when the binder dies`() {
        monitor.start()
        assertEquals(ShizukuState.Ready, monitor.state)

        gateway.running = false
        gateway.emitBinderDead()

        assertEquals(ShizukuState.NotRunning, monitor.state)
    }

    @Test
    fun `moves to ready once the permission request is granted`() {
        gateway.permission = DENIED
        monitor.start()
        assertEquals(ShizukuState.PermissionRequired, monitor.state)

        gateway.permission = GRANTED
        gateway.emitPermissionResult(PERMISSION_REQUEST_CODE, GRANTED)

        assertEquals(ShizukuState.Ready, monitor.state)
    }

    @Test
    fun `stays in permission required when the request is denied`() {
        gateway.permission = DENIED
        monitor.start()

        gateway.emitPermissionResult(PERMISSION_REQUEST_CODE, DENIED)

        assertEquals(ShizukuState.PermissionRequired, monitor.state)
    }

    @Test
    fun `requests permission only when one is actually required`() {
        monitor.start()
        monitor.requestPermission()

        assertEquals(emptyList<Int>(), gateway.permissionRequests)

        gateway.permission = DENIED
        monitor.refresh()
        monitor.requestPermission()

        assertEquals(listOf(PERMISSION_REQUEST_CODE), gateway.permissionRequests)
    }

    @Test
    fun `removes every listener it added when stopped`() {
        monitor.start()
        assertEquals(3, gateway.liveListenerCount)

        monitor.stop()

        assertEquals(0, gateway.liveListenerCount)
    }
}
