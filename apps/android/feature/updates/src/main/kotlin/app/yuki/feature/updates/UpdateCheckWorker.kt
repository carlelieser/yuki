package app.yuki.feature.updates

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val check: BackgroundUpdateCheck,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = when (check.run()) {
        BackgroundCheckOutcome.Checked -> Result.success()
        BackgroundCheckOutcome.Unreachable -> Result.retry()
    }
}
