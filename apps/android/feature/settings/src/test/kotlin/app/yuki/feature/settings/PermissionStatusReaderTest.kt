package app.yuki.feature.settings

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatusReaderTest {
    @Test
    fun installUnknownAppsReadsTheAppOpNotTheRuntimeGrant() {
        val lookup = RecordingLookup(grantedKinds = setOf(PermissionKind.InstallPackagesAppOp))

        val status = statusOf(permissionFor(Manifest.permission.REQUEST_INSTALL_PACKAGES), SDK, lookup)

        assertEquals(PermissionStatus.Granted, status)
        assertEquals(listOf(PermissionKind.InstallPackagesAppOp), lookup.kindsAsked)
    }

    @Test
    fun installUnknownAppsIsDeniedWhenTheAppOpIsNotGranted() {
        val lookup = RecordingLookup(grantedKinds = setOf(PermissionKind.Runtime))

        val status = statusOf(permissionFor(Manifest.permission.REQUEST_INSTALL_PACKAGES), SDK, lookup)

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun aRuntimePermissionStillReadsTheRuntimeGrant() {
        val lookup = RecordingLookup(grantedKinds = setOf(PermissionKind.Runtime))

        val status = statusOf(permissionFor(Manifest.permission.INTERNET), SDK, lookup)

        assertEquals(PermissionStatus.Granted, status)
        assertEquals(listOf(PermissionKind.Runtime), lookup.kindsAsked)
    }

    @Test
    fun notificationsCountAsGrantedBelowTheSdkThatIntroducedThem() {
        val lookup = RecordingLookup(grantedKinds = emptySet())
        val notifications = permissionFor(Manifest.permission.POST_NOTIFICATIONS)

        val status = statusOf(notifications, Build.VERSION_CODES.S, lookup)

        assertEquals(PermissionStatus.Granted, status)
        assertEquals(emptyList<PermissionKind>(), lookup.kindsAsked)
    }

    @Test
    fun notificationsReadTheRuntimeGrantOnceTheSdkSupportsThem() {
        val lookup = RecordingLookup(grantedKinds = emptySet())
        val notifications = permissionFor(Manifest.permission.POST_NOTIFICATIONS)

        val status = statusOf(notifications, Build.VERSION_CODES.TIRAMISU, lookup)

        assertEquals(PermissionStatus.Denied, status)
        assertEquals(listOf(PermissionKind.Runtime), lookup.kindsAsked)
    }
}

private const val SDK = Build.VERSION_CODES.TIRAMISU

private class RecordingLookup(private val grantedKinds: Set<PermissionKind>) :
    PlatformPermissionLookup {
    val kindsAsked: MutableList<PermissionKind> = mutableListOf()

    override fun isGranted(kind: PermissionKind, permission: String): Boolean {
        kindsAsked += kind
        return kind in grantedKinds
    }
}

private fun permissionFor(permission: String): AppPermission =
    YUKI_PERMISSIONS.first { entry -> entry.permission == permission }
