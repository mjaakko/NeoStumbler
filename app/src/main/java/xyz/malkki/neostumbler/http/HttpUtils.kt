package xyz.malkki.neostumbler.http

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import xyz.malkki.neostumbler.BuildConfig
import xyz.malkki.neostumbler.R
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object HttpUtils {
    val CONNECT_TIMEOUT = 30.seconds
    /* Read timeout should be long enough, because the Geosubmit API responds only when all data has been processed and
     that might take a while if a large amount of reports is sent at once */
    val READ_TIMEOUT = 2.minutes

    fun getUserAgent(context: Context): String {
        val userAgentVersion = if (BuildConfig.DEBUG) {
            "dev"
        } else {
            BuildConfig.VERSION_CODE
        }

        return "${context.resources.getString(R.string.app_name)}/${userAgentVersion}"
    }

    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                addInterceptor(UserAgentInterceptor(getUserAgent(context)))

                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor(Timber::d).apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }

                connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
                readTimeout(READ_TIMEOUT.toJavaDuration())
            }
            .build()
    }
}