package xyz.malkki.neostumbler.network

import android.content.Context
import okhttp3.Call
import xyz.malkki.neostumbler.http.HttpUtils

class OkHttpCallFactoryProvider(context: Context) : HttpCallFactoryProvider {
    private val okHttpClient by lazy { HttpUtils.createOkHttpClient(context.applicationContext) }

    override suspend fun getHttpCallFactory(): Call.Factory {
        return okHttpClient
    }
}
