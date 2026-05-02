package com.dentical.staff.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) {
    private val cm = context.getSystemService<ConnectivityManager>()!!

    val isConnected: Boolean
        get() = cm.activeNetwork
            ?.let { cm.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
