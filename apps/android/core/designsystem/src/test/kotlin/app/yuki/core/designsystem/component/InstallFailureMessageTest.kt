package app.yuki.core.designsystem.component

import app.yuki.core.designsystem.R
import app.yuki.core.model.InstallFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallFailureMessageTest {
    @Test
    fun `a download without a status asks the user to check their connection`() {
        assertEquals(
            R.string.designsystem_install_error_download_offline,
            installFailureMessage(InstallFailure.DownloadFailed(httpStatus = null)),
        )
    }

    @Test
    fun `a missing release says it is no longer available`() {
        listOf(404, 410).forEach { status ->
            assertEquals(
                R.string.designsystem_install_error_download_gone,
                installFailureMessage(InstallFailure.DownloadFailed(status)),
            )
        }
    }

    @Test
    fun `any other http error blames the server`() {
        listOf(403, 500, 503).forEach { status ->
            assertEquals(
                R.string.designsystem_install_error_download_server,
                installFailureMessage(InstallFailure.DownloadFailed(status)),
            )
        }
    }

    @Test
    fun `every failure has its own message`() {
        val failures = listOf(
            InstallFailure.Unexpected,
            InstallFailure.DownloadUnreadable,
            InstallFailure.NotAnApk,
            InstallFailure.Aborted,
            InstallFailure.SessionFailed,
            InstallFailure.InsufficientStorage,
            InstallFailure.Incompatible,
            InstallFailure.InvalidApk,
            InstallFailure.PackageMismatch,
            InstallFailure.SignatureConflict,
            InstallFailure.TimedOut,
            InstallFailure.ConfirmationTimedOut,
            InstallFailure.ScheduleFailed,
            InstallFailure.Rejected("blocked"),
        )

        val messages = failures.map(::installFailureMessage)

        assertEquals(failures.size, messages.toSet().size)
    }
}
