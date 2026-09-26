package app.yuki.feature.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCheckSchedulerTest {
    private val work = RecordingUpdateCheckWork()
    private val isAutomatic = MutableStateFlow(true)
    private val includePrereleases = MutableStateFlow(false)
    private val scheduler = UpdateCheckScheduler(
        work = work,
        automatic = { isAutomatic },
        prereleases = { includePrereleases },
    )

    @Test
    fun automaticChecksScheduleThePeriodicCheck() = runTest {
        following {
            assertEquals(listOf("schedule"), work.calls)
        }
    }

    @Test
    fun turningAutomaticChecksOffCancelsThem() = runTest {
        following {
            isAutomatic.value = false
            runCurrent()

            assertEquals(listOf("schedule", "cancel"), work.calls)
        }
    }

    @Test
    fun changingThePrereleaseChoiceChecksAgainRightAway() = runTest {
        following {
            includePrereleases.value = true
            runCurrent()

            assertEquals(listOf("schedule", "schedule", "runOnce"), work.calls)
        }
    }

    @Test
    fun changingThePrereleaseChoiceWhileChecksAreOffDoesNotCheck() = runTest {
        isAutomatic.value = false

        following {
            includePrereleases.value = true
            runCurrent()

            assertEquals(listOf("cancel", "cancel"), work.calls)
        }
    }

    private fun TestScope.following(block: () -> Unit) {
        val job = launch { scheduler.follow() }
        runCurrent()
        block()
        job.cancel()
    }
}

private class RecordingUpdateCheckWork : UpdateCheckWork {
    val calls: MutableList<String> = mutableListOf()

    override suspend fun schedule() {
        calls += "schedule"
    }

    override suspend fun runOnce() {
        calls += "runOnce"
    }

    override suspend fun cancel() {
        calls += "cancel"
    }
}
