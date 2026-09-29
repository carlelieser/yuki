package app.yuki.core.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Singleton
class BrowserAuthResults @Inject constructor() {
    private val latest = MutableStateFlow<BrowserAuthReturn?>(null)

    fun deliver(url: String?) {
        parseBrowserAuthReturn(url)?.let { returned -> latest.value = returned }
    }

    internal fun returns(): Flow<BrowserAuthReturn> = latest.filterNotNull()

    internal fun consume(returned: BrowserAuthReturn) {
        latest.compareAndSet(returned, null)
    }
}

class BrowserAuth @Inject constructor(
    private val pending: PendingBrowserAuth,
    private val results: BrowserAuthResults,
) {
    suspend fun begin(purpose: BrowserAuthPurpose): String = pending.begin(purpose)

    fun outcomes(purpose: BrowserAuthPurpose): Flow<BrowserAuthResult> = results.returns()
        .filter { returned -> returned.purpose == purpose }
        .onEach(results::consume)
        .map { returned -> claimed(returned.result, purpose) }

    private suspend fun claimed(result: BrowserAuthResult, purpose: BrowserAuthPurpose) =
        if (pending.claim(result.state, purpose)) {
            result
        } else {
            BrowserAuthResult.Failed(BROWSER_AUTH_EXPIRED, result.state)
        }
}
