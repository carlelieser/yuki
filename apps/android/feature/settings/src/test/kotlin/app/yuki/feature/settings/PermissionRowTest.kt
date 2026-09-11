package app.yuki.feature.settings

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRowTest {
    @Test
    fun aGrantedPermissionDescribesItselfAsGranted() {
        val row = rowFor(requiredPermission(), PermissionStatus.Granted)

        assertTrue(row.isGranted)
        assertEquals(STATUS_GRANTED, row.statusLabel)
    }

    @Test
    fun aDeniedPermissionDescribesItselfAsDeniedWhetherOrNotItIsRequired() {
        val required = rowFor(requiredPermission(), PermissionStatus.Denied)
        val optional = rowFor(optionalPermission(), PermissionStatus.Denied)

        assertEquals(STATUS_DENIED, required.statusLabel)
        assertEquals(STATUS_DENIED, optional.statusLabel)
    }

    @Test
    fun everyPlannedPermissionIsPresentWithItsReason() {
        val permissions = YUKI_PERMISSIONS.associateBy(AppPermission::permission)

        assertEquals(4, YUKI_PERMISSIONS.size)
        assertEquals(
            "Browse listings and download apps",
            permissions.getValue(Manifest.permission.INTERNET).reason,
        )
        assertEquals(
            "Install apps when Shizuku is unavailable",
            permissions.getValue(Manifest.permission.REQUEST_INSTALL_PACKAGES).reason,
        )
        assertEquals(
            "Notify when a download or install finishes",
            permissions.getValue(Manifest.permission.POST_NOTIFICATIONS).reason,
        )
        assertEquals(
            "Install silently through Shizuku",
            permissions.getValue(SHIZUKU_PERMISSION).reason,
        )
    }

    @Test
    fun onlyInternetAndInstallPackagesAreRequired() {
        val required = YUKI_PERMISSIONS.filter(AppPermission::isRequired).map(AppPermission::permission)

        assertEquals(
            listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.REQUEST_INSTALL_PACKAGES,
            ),
            required,
        )
    }

    @Test
    fun installUnknownAppsIsDeclaredAsAnAppOpNotARuntimePermission() {
        val permissions = YUKI_PERMISSIONS.associateBy(AppPermission::permission)

        assertEquals(
            PermissionKind.InstallPackagesAppOp,
            permissions.getValue(Manifest.permission.REQUEST_INSTALL_PACKAGES).kind,
        )
        assertEquals(
            PermissionKind.Runtime,
            permissions.getValue(Manifest.permission.INTERNET).kind,
        )
        assertEquals(
            PermissionKind.Runtime,
            permissions.getValue(Manifest.permission.POST_NOTIFICATIONS).kind,
        )
    }

    @Test
    fun aDeniedPermissionRoutesToItsSystemSettingsPage() {
        val destinations = RecordingSystemDestinations()
        var permissionRequests = 0
        val handler = permissionClickHandler(destinations) { permissionRequests += 1 }

        handler(rowFor(requiredPermission(), PermissionStatus.Denied))

        assertEquals(listOf(Manifest.permission.INTERNET), destinations.settingsOpened)
        assertEquals(0, permissionRequests)
    }

    @Test
    fun theShizukuRowRoutesToThePermissionRequestNotSystemSettings() {
        val destinations = RecordingSystemDestinations()
        var permissionRequests = 0
        val handler = permissionClickHandler(destinations) { permissionRequests += 1 }

        handler(rowFor(shizukuPermission(), PermissionStatus.Denied))

        assertTrue(destinations.settingsOpened.isEmpty())
        assertEquals(1, permissionRequests)
    }

    @Test
    fun aGrantedRowDoesNothingWhenTapped() {
        val destinations = RecordingSystemDestinations()
        var permissionRequests = 0
        val handler = permissionClickHandler(destinations) { permissionRequests += 1 }

        handler(rowFor(requiredPermission(), PermissionStatus.Granted))
        handler(rowFor(shizukuPermission(), PermissionStatus.Granted))

        assertTrue(destinations.settingsOpened.isEmpty())
        assertEquals(0, permissionRequests)
    }
}

private fun rowFor(permission: AppPermission, status: PermissionStatus): PermissionRow =
    PermissionRow(permission = permission, status = status)

private fun requiredPermission(): AppPermission =
    YUKI_PERMISSIONS.first { entry -> entry.permission == Manifest.permission.INTERNET }

private fun optionalPermission(): AppPermission =
    YUKI_PERMISSIONS.first { entry -> entry.permission == Manifest.permission.POST_NOTIFICATIONS }

private fun shizukuPermission(): AppPermission =
    YUKI_PERMISSIONS.first { entry -> entry.permission == SHIZUKU_PERMISSION }
