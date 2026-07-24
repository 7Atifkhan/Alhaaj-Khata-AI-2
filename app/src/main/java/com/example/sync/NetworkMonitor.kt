package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    ONLINE,
    OFFLINE,
    SYNCING,
    WAITING,
    SYNC_FAILED
}

data class NetworkState(
    val status: ConnectionStatus = ConnectionStatus.ONLINE,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val lastStateChange: Long = System.currentTimeMillis()
)

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(getInitialNetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            
            _networkState.value = NetworkState(
                status = ConnectionStatus.ONLINE,
                isWifi = isWifi,
                isCellular = isCellular,
                lastStateChange = System.currentTimeMillis()
            )
        }

        override fun onLost(network: Network) {
            _networkState.value = NetworkState(
                status = ConnectionStatus.OFFLINE,
                isWifi = false,
                isCellular = false,
                lastStateChange = System.currentTimeMillis()
            )
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isOnline = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            _networkState.value = _networkState.value.copy(
                status = if (isOnline) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE,
                isWifi = isWifi,
                isCellular = isCellular
            )
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Fallback for permissions / restricted environments
        }
    }

    private fun getInitialNetworkState(): NetworkState {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            NetworkState(
                status = if (isOnline) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE,
                isWifi = isWifi,
                isCellular = isCellular
            )
        } catch (_: Exception) {
            NetworkState(status = ConnectionStatus.ONLINE)
        }
    }

    fun updateStatus(status: ConnectionStatus) {
        _networkState.value = _networkState.value.copy(status = status)
    }

    fun isOnline(): Boolean {
        return _networkState.value.status != ConnectionStatus.OFFLINE
    }
}
