package app.yuki.core.installer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserActionPromptTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prompt = UserActionPrompt(context)

    @After
    fun withdraw() {
        prompt.dismiss(SESSION_ID)
    }

    @Test
    fun anInstallAwaitingTheUserPostsATapToConfirmNotification() {
        prompt.post(SESSION_ID, confirmIntent())

        val posted = awaitPosted()
        assertEquals(1, posted.size)
        assertEquals(CONFIRMATION_CHANNEL_ID, posted.single().notification.channelId)
        assertTrue(posted.single().notification.contentIntent != null)
    }

    @Test
    fun dismissingThePromptWithdrawsTheNotification() {
        prompt.post(SESSION_ID, confirmIntent())

        prompt.dismiss(SESSION_ID)

        assertTrue(awaitWithdrawn())
    }

    private fun activeConfirmations() = context.getSystemService(NotificationManager::class.java)
        .activeNotifications
        .filter { posted -> posted.tag == CONFIRMATION_NOTIFICATION_TAG && posted.id == SESSION_ID }

    private fun awaitPosted() = awaitConfirmations { posted -> posted.isNotEmpty() }

    private fun awaitConfirmations(
        isSettled: (List<StatusBarNotification>) -> Boolean,
    ): List<StatusBarNotification> {
        val deadline = SystemClock.uptimeMillis() + NOTIFICATION_TIMEOUT_MILLIS
        while (!isSettled(activeConfirmations()) && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_MILLIS)
        }

        return activeConfirmations()
    }

    private fun awaitWithdrawn(): Boolean =
        awaitConfirmations { posted -> posted.isEmpty() }.isEmpty()

    private fun confirmIntent(): Intent =
        Intent(Intent.ACTION_VIEW).setClassName(context, "app.yuki.core.installer.Missing")
}

private const val SESSION_ID = 4_242
private const val NOTIFICATION_TIMEOUT_MILLIS = 2_000L
private const val POLL_MILLIS = 50L
