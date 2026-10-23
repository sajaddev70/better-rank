package com.marveloushost.betterrank.networkMonitor

import android.content.Context
import android.widget.Toast
import com.marveloushost.betterrank.R
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class NetworkMonitorInterceptor @Inject constructor(private val liveNetworkMonitor: NetworkMonitor,
                                                    @ApplicationContext private val context: Context): Interceptor {


    @Throws(NoNetworkException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        if(liveNetworkMonitor.isConnected()){
            return chain.proceed(request)
        }else{
            throw NoNetworkException(context.getString(R.string.check_your_network_connection))
        }

    }
}