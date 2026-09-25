package app.yuki.core.installer

import android.content.pm.PackageInstaller
import app.yuki.core.model.InstallFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStatusMappingTest {
    @Test
    fun `success maps to succeeded`() {
        assertEquals(InstallOutcome.Succeeded, outcomeOf(PackageInstaller.STATUS_SUCCESS))
    }

    @Test
    fun `pending user action maps to awaiting user action`() {
        assertEquals(
            InstallOutcome.AwaitingUserAction,
            outcomeOf(PackageInstaller.STATUS_PENDING_USER_ACTION),
        )
    }

    @Test
    fun `aborted maps to the aborted failure`() {
        assertEquals(failed(InstallFailure.Aborted), outcomeOf(FAILURE_ABORTED))
    }

    @Test
    fun `storage failure maps to insufficient storage`() {
        assertEquals(failed(InstallFailure.InsufficientStorage), outcomeOf(FAILURE_STORAGE))
    }

    @Test
    fun `incompatible maps to incompatible`() {
        assertEquals(failed(InstallFailure.Incompatible), outcomeOf(FAILURE_INCOMPATIBLE))
    }

    @Test
    fun `invalid maps to invalid apk`() {
        assertEquals(failed(InstallFailure.InvalidApk), outcomeOf(FAILURE_INVALID))
    }

    @Test
    fun `conflict maps to signature conflict`() {
        assertEquals(failed(InstallFailure.SignatureConflict), outcomeOf(FAILURE_CONFLICT))
    }

    @Test
    fun `blocked keeps the installer message`() {
        assertEquals(
            failed(InstallFailure.Rejected("blocked by policy")),
            outcomeOf(FAILURE_BLOCKED, "blocked by policy"),
        )
    }

    @Test
    fun `an unknown status without a message is described by its code`() {
        assertEquals(failed(InstallFailure.Rejected("PackageInstaller status 99")), outcomeOf(99))
    }
}

private const val FAILURE_ABORTED = PackageInstaller.STATUS_FAILURE_ABORTED
private const val FAILURE_STORAGE = PackageInstaller.STATUS_FAILURE_STORAGE
private const val FAILURE_INCOMPATIBLE = PackageInstaller.STATUS_FAILURE_INCOMPATIBLE
private const val FAILURE_INVALID = PackageInstaller.STATUS_FAILURE_INVALID
private const val FAILURE_CONFLICT = PackageInstaller.STATUS_FAILURE_CONFLICT
private const val FAILURE_BLOCKED = PackageInstaller.STATUS_FAILURE_BLOCKED

private fun failed(reason: InstallFailure): InstallOutcome = InstallOutcome.Failed(reason)

private fun outcomeOf(code: Int, message: String? = null): InstallOutcome =
    SessionStatus(sessionId = 1, code = code, message = message, userAction = null).toOutcome()
