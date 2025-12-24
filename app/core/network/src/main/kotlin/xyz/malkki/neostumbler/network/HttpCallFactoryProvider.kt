package xyz.malkki.neostumbler.network

import okhttp3.Call

fun interface HttpCallFactoryProvider {
    suspend fun getHttpCallFactory(): Call.Factory
}
