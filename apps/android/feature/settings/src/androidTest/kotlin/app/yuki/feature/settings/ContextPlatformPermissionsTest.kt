package app.yuki.feature.settings

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextPlatformPermissionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val platform = ContextPlatformPermissions(context)

    @Test
    fun theAppOpLookupAgreesWithThePackageManager() {
        assertEquals(
            context.packageManager.canRequestPackageInstalls(),
            platform.canRequestPackageInstalls(),
        )
    }

    @Test
    fun theAppOpLookupIsNotTheRuntimeCheckInDisguise() {
        val reader = ContextPermissionStatusReader(platform)
        val installUnknownApps = YUKI_PERMISSIONS
            .first { entry -> entry.permission == Manifest.permission.REQUEST_INSTALL_PACKAGES }

        val expected = if (context.packageManager.canRequestPackageInstalls()) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied
        }

        assertEquals(expected, reader.statusOf(installUnknownApps))
    }

    @Test
    fun aDeclaredManifestPermissionReadsAsGrantedThroughTheRuntimeLookup() {
        assertTrue(platform.isRuntimeGranted(Manifest.permission.INTERNET))
    }

    @Test
    fun theSdkLevelComesFromTheRunningDevice() {
        assertTrue(platform.sdkInt >= MIN_SDK)
    }
}

private const val MIN_SDK = 26
