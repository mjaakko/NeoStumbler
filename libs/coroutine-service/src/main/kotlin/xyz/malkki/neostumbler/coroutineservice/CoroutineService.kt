package xyz.malkki.neostumbler.coroutineservice

import android.app.Service
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber

abstract class CoroutineService(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    Service() {
    protected lateinit var serviceScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "Unhandled exception in the coroutine scope, stopping service")

            stopSelf()
        }

        serviceScope = CoroutineScope(dispatcher + exceptionHandler)
    }

    override fun onDestroy() {
        serviceScope.cancel()

        super.onDestroy()
    }
}
