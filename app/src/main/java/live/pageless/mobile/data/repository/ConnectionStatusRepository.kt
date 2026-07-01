package live.pageless.mobile.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionStatus {
    Connected,
    NoInternet,
    ServerUnavailable,
}

@Singleton
class ConnectionStatusRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        private val hasInternet = MutableStateFlow(currentHasInternet())
        private val serverReachable = MutableStateFlow<Boolean?>(null)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val status: StateFlow<ConnectionStatus> =
            combine(hasInternet, serverReachable) { internet, reachable ->
                when {
                    !internet -> ConnectionStatus.NoInternet
                    reachable == false -> ConnectionStatus.ServerUnavailable
                    else -> ConnectionStatus.Connected
                }
            }.stateIn(scope, SharingStarted.Eagerly, ConnectionStatus.Connected)

        init {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        hasInternet.value = currentHasInternet()
                    }

                    override fun onLost(network: Network) {
                        hasInternet.value = false
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        hasInternet.value = currentHasInternet()
                    }
                },
            )
        }

        fun markServerSuccess() {
            hasInternet.value = currentHasInternet()
            serverReachable.value = true
        }

        fun markServerFailure() {
            val internet = currentHasInternet()
            hasInternet.value = internet
            if (internet) serverReachable.value = false
        }

        private fun currentHasInternet(): Boolean {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
