package com.nilam.iptv.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class ConnectivityObserver(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun interface Listener {
        fun onStatusChanged(isConnected: Boolean)
    }

    fun start(listener: Listener) {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = listener.onStatusChanged(true)
            override fun onLost(network: Network) = listener.onStatusChanged(false)
            override fun onUnavailable() = listener.onStatusChanged(false)
        }
        cm.registerNetworkCallback(request, networkCallback!!)
        listener.onStatusChanged(isCurrentlyConnected())
    }

    fun stop() {
        networkCallback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        networkCallback = null
    }

    fun isCurrentlyConnected(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
