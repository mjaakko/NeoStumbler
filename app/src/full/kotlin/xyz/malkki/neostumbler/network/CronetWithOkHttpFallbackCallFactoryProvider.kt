package xyz.malkki.neostumbler.network

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.google.net.cronet.okhttptransport.CronetCallFactory
import kotlin.io.path.createDirectories
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import timber.log.Timber
import xyz.malkki.neostumbler.http.HttpUtils

class CronetWithOkHttpFallbackCallFactoryProvider(context: Context) : HttpCallFactoryProvider {
    private val appContext = context.applicationContext

    @OptIn(DelicateCoroutinesApi::class)
    private val deferredCallFactory: Deferred<Call.Factory> =
        GlobalScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                CronetProviderInstaller.installProvider(appContext).await()
            } catch (ex: Exception) {
                Timber.w("Failed to install Cronet provider, falling back to OkHttp: ${ex.message}")

                return@async HttpUtils.createOkHttpClient(appContext)
            }

            val provider =
                CronetProvider.getAllProviders(appContext).find { provider ->
                    provider.isEnabled && provider.name != CronetProvider.PROVIDER_NAME_FALLBACK
                }

            if (provider == null) {
                Timber.w("Cronet is not available, falling back to OkHttp")

                return@async HttpUtils.createOkHttpClient(appContext)
            }

            val cacheDir =
                appContext.cacheDir.toPath().resolve("cronet_cache").apply { createDirectories() }

            val userAgent = HttpUtils.getUserAgent(appContext)

            val cronetEngine =
                provider
                    .createBuilder()
                    .enableBrotli(true)
                    .enableHttp2(true)
                    .enableQuic(true)
                    .setUserAgent(userAgent)
                    .setStoragePath(cacheDir.toAbsolutePath().toString())
                    .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, HttpUtils.CACHE_SIZE)
                    .build()

            return@async CronetCallFactory.newBuilder(cronetEngine)
                .setReadTimeoutMillis(HttpUtils.READ_TIMEOUT.inWholeMilliseconds.toInt())
                .build()
        }

    override suspend fun getHttpCallFactory(): Call.Factory {
        return deferredCallFactory.await()
    }
}
