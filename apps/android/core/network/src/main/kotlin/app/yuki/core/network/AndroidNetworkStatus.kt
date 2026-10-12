package app.yuki.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.yuki.core.model.NetworkStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidNetworkStatus @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkStatus {
    override fun isOnline(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
