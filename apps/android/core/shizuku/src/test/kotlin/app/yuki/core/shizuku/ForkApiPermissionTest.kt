package app.yuki.core.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForkApiPermissionTest {
    @Test
    fun `matches the upstream shizuku permission`() {
        assertTrue(isForkApiPermission("moe.shizuku.manager.permission.API_V23"))
    }

    @Test
    fun `matches rebranded fork permissions`() {
        listOf(
            "xyz.shizuku.extra.permission.API_V23",
            "af.shizuku.plus.permission.API_V23",
            "com.hamondev.shevery.permission.API_V23",
        ).forEach { permission ->
            assertTrue(permission, isForkApiPermission(permission))
        }
    }

    @Test
    fun `ignores manager permissions that do not expose the api`() {
        assertFalse(isForkApiPermission("xyz.shizuku.extra.permission.MANAGER"))
    }

    @Test
    fun `ignores unrelated permissions`() {
        listOf(
            "android.permission.INTERNET",
            "com.example.permission.API_V23_EXTRA",
            "API_V23",
        ).forEach { permission ->
            assertFalse(permission, isForkApiPermission(permission))
        }
    }
}
