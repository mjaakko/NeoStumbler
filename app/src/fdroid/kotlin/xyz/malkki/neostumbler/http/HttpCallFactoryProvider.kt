package xyz.malkki.neostumbler.http

import android.content.Context
import okhttp3.Call

suspend fun getCallFactory(context: Context): Call.Factory {
    return HttpUtils.createOkHttpClient(context)
}
