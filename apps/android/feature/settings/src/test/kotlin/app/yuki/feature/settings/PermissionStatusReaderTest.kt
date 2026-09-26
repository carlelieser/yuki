package app.yuki.feature.settings

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusReaderTest {
    @Test
    fun installUnknownAppsIsGrantedWhenOnlyTheAppOpIsGranted() {
        val platform = FakePlatformPermissions(canRequestPackageInstalls = true)

        val status = readerFor(platform).statusOf(installUnknownApps())

        assertEquals(PermissionStatus.Granted, status)
    }

    @Test
    fun installUnknownAppsIgnoresTheRuntimeGrantEntirely() {
        val platform = FakePlatformPermissions(
            grantedRuntimePermissions = setOf(Manifest.permission.REQUEST_INSTALL_PACKAGES),
            canRequestPackageInstalls = false,
        )

        val status = readerFor(platform).statusOf(installUnknownApps())

        assertEquals(PermissionStatus.Denied, status)
        assertTrue(platform.runtimeChecks.isEmpty())
        assertEquals(1, platform.packageInstallChecks)
    }

    @Test
    fun installUnknownAppsIsDeniedWhenTheAppOpIsNotGranted() {
        val platform = FakePlatformPermissions(canRequestPackageInstalls = false)

        val status = readerFor(platform).statusOf(installUnknownApps())

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun aRuntimePermissionReadsTheRuntimeGrantNotTheAppOp() {
        val platform = FakePlatformPermissions(
            grantedRuntimePermissions = setOf(Manifest.permission.INTERNET),
            canRequestPackageInstalls = false,
        )

        val status = readerFor(platform).statusOf(permissionFor(Manifest.permission.INTERNET))

        assertEquals(PermissionStatus.Granted, status)
        assertEquals(listOf(Manifest.permission.INTERNET), platform.runtimeChecks)
        assertEquals(0, platform.packageInstallChecks)
    }

    @Test
    fun aRuntimePermissionIsDeniedWhenOnlyTheAppOpIsGranted() {
        val platform = FakePlatformPermissions(canRequestPackageInstalls = true)

        val status = readerFor(platform).statusOf(permissionFor(Manifest.permission.INTERNET))

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun notificationsReadTheRuntimeGrant() {
        val platform = FakePlatformPermissions()

        val status = readerFor(platform).statusOf(notifications())

        assertEquals(PermissionStatus.Denied, status)
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), platform.runtimeChecks)
    }

    @Test
    fun everyDeclaredPermissionIsReadableWithoutTouchingTheWrongPlatformCall() {
        val platform = FakePlatformPermissions()

        PERMISSIONS.forEach { permission -> readerFor(platform).statusOf(permission) }

        assertFalse(platform.runtimeChecks.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES))
        assertEquals(1, platform.packageInstallChecks)
    }
}

private class FakePlatformPermissions(
    private val grantedRuntimePermissions: Set<String> = emptySet(),
    private val canRequestPackageInstalls: Boolean = false,
) : PlatformPermissions {
    val runtimeChecks: MutableList<String> = mutableListOf()

    var packageInstallChecks: Int = 0
        private set

    override fun isRuntimeGranted(permission: String): Boolean {
        runtimeChecks += permission
        return permission in grantedRuntimePermissions
    }

    override fun canRequestPackageInstalls(): Boolean {
        packageInstallChecks += 1
        return canRequestPackageInstalls
    }
}

private fun readerFor(platform: PlatformPermissions): PermissionStatusReader =
    ContextPermissionStatusReader(platform)

private fun installUnknownApps(): AppPermission =
    permissionFor(Manifest.permission.REQUEST_INSTALL_PACKAGES)

private fun notifications(): AppPermission = permissionFor(Manifest.permission.POST_NOTIFICATIONS)

private fun permissionFor(permission: String): AppPermission =
    PERMISSIONS.first { entry -> entry.permission == permission }

private val PERMISSIONS = yukiPermissions(Build.VERSION_CODES.TIRAMISU)
