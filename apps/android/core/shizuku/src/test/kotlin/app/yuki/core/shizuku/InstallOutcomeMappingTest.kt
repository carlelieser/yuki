package app.yuki.core.shizuku

import app.yuki.core.installer.InstallOutcome
import app.yuki.core.model.InstallFailure
import org.junit.Assert.assertEquals
import org.junit.Test

private const val STATUS_SUCCESS = INSTALL_STATUS_SUCCESS
private const val STATUS_FAILURE = INSTALL_STATUS_FAILURE

class InstallOutcomeMappingTest {
    @Test
    fun `maps a success status to a succeeded outcome`() {
        assertEquals(InstallOutcome.Succeeded, toOutcome(STATUS_SUCCESS, null))
    }

    @Test
    fun `maps a storage failure to insufficient storage`() {
        assertEquals(
            InstallOutcome.Failed(InstallFailure.InsufficientStorage),
            toOutcome(STATUS_FAILURE, "INSTALL_FAILED_INSUFFICIENT_STORAGE"),
        )
    }

    @Test
    fun `maps an invalid apk to invalid apk`() {
        assertEquals(
            InstallOutcome.Failed(InstallFailure.InvalidApk),
            toOutcome(STATUS_FAILURE, "INSTALL_FAILED_INVALID_APK"),
        )
    }

    @Test
    fun `maps a signature clash to a package mismatch`() {
        assertEquals(
            InstallOutcome.Failed(InstallFailure.PackageMismatch),
            toOutcome(STATUS_FAILURE, "INSTALL_FAILED_UPDATE_INCOMPATIBLE: SIGNATURES do not match"),
        )
    }

    @Test
    fun `maps an incompatible update without a signature clash to incompatible`() {
        assertEquals(
            InstallOutcome.Failed(InstallFailure.Incompatible),
            toOutcome(STATUS_FAILURE, "INSTALL_FAILED_UPDATE_INCOMPATIBLE: downgrade"),
        )
    }

    @Test
    fun `keeps an unrecognised failure message rather than discarding it`() {
        assertEquals(
            InstallOutcome.Failed(InstallFailure.Rejected("pm died unexpectedly")),
            toOutcome(STATUS_FAILURE, "pm died unexpectedly"),
        )
    }

    @Test
    fun `maps a failure with no message to aborted`() {
        assertEquals(InstallOutcome.Failed(InstallFailure.Aborted), toOutcome(STATUS_FAILURE, null))
    }

    @Test
    fun `never awaits user action on the silent path`() {
        val statuses = listOf(STATUS_SUCCESS, STATUS_FAILURE, 3, -1)

        val outcomes = statuses.map { status -> toOutcome(status, "any message") }

        assertEquals(emptyList<InstallOutcome>(), outcomes.filterIsInstance<InstallOutcome.AwaitingUserAction>())
    }
}
