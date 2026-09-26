package app.yuki.feature.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNotificationSwitchTest {
    private val isEnabled = MutableStateFlow(true)
    private val notifier = RecordingUpdateNotifier()
    private val switch = UpdateNotificationSwitch(preference = { isEnabled }, notifier = notifier)

    @Test
    fun keepingNotificationsOnLeavesTheNotificationAlone() = runTest {
        val job = launch { switch.follow() }
        runCurrent()

        assertEquals(0, notifier.dismissals)
        job.cancel()
    }

    @Test
    fun turningNotificationsOffDismissesTheNotification() = runTest {
        val job = launch { switch.follow() }
        runCurrent()

        isEnabled.value = false
        runCurrent()

        assertEquals(1, notifier.dismissals)
        job.cancel()
    }
}
