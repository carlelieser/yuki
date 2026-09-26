package app.yuki.feature.updates

import javax.inject.Inject

internal class UpdatesDependencies @Inject constructor(
    val check: UpdateCheck,
    val installer: UpdateInstaller,
    val notifier: UpdateNotifier,
)
