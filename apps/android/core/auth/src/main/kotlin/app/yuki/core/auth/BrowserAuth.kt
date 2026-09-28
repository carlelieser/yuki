package app.yuki.core.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

@Singleton
class BrowserAuthResults @Inject constructor() {
    private val latest = MutableStateFlow<BrowserAuthResult?>(null)

    fun deliver(url: String?) {
        parseBrowserAuthResult(url)?.let { result -> latest.value = result }
    }

    internal fun results(): Flow<BrowserAuthResult> = latest.filterNotNull()

    internal fun consume(result: BrowserAuthResult) {
        latest.compareAndSet(result, null)
    }
}

class BrowserAuth @Inject constructor(
    private val pending: PendingBrowserAuth,
    private val results: BrowserAuthResults,
) {
    suspend fun begin(purpose: BrowserAuthPurpose): String = pending.begin(purpose)

    fun outcomes(purpose: BrowserAuthPurpose): Flow<BrowserAuthResult> = results.results()
        .filter { result -> pending.claim(result.state, purpose) }
        .onEach(results::consume)
}
