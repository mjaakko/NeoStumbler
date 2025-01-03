package xyz.malkki.neostumbler.http

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.google.net.cronet.okhttptransport.CronetCallFactory
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import timber.log.Timber
import kotlin.io.path.createDirectories

suspend fun getCallFactory(context: Context): Call.Factory {
    try {
        CronetProviderInstaller.installProvider(context).await()
    } catch (ex: Exception) {
        Timber.w("Failed to install Cronet provider, falling back to OkHttp: ${ex.message}")

        return HttpUtils.createOkHttpClient(context)
    }

    val provider = CronetProvider.getAllProviders(context).find { provider ->
        provider.isEnabled && provider.name != CronetProvider.PROVIDER_NAME_FALLBACK
    }

    if (provider == null) {
        Timber.w("Cronet is not available, falling back to OkHttp")

        return HttpUtils.createOkHttpClient(context)
    }

    val cacheDir = context.cacheDir.toPath().resolve("cronet_cache").apply {
        createDirectories()
    }

    val userAgent = HttpUtils.getUserAgent(context)

    val cronetEngine = provider.createBuilder()
        .enableBrotli(true)
        .enableHttp2(true)
        .enableQuic(true)
        .setUserAgent(userAgent)
        .setStoragePath(cacheDir.toAbsolutePath().toString())
        .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, HttpUtils.CACHE_SIZE)
        .build()

    return CronetCallFactory.newBuilder(cronetEngine)
        .setReadTimeoutMillis(HttpUtils.READ_TIMEOUT.inWholeMilliseconds.toInt())
        .build()
}