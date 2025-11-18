package xyz.malkki.neostumbler.coroutinebroadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

// Broadcast receivers have 10 seconds to finish their work
// -> https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()
private val BROADCAST_TIMEOUT = 10.seconds

abstract class CoroutineBroadcastReceiver(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : BroadcastReceiver() {
    protected abstract suspend fun handleIntent(context: Context, intent: Intent)

    final override fun onReceive(context: Context, intent: Intent) {
        val pendingResult: PendingResult = goAsync()

        // Safe to use GlobalScope here, because we have a timeout
        @OptIn(DelicateCoroutinesApi::class)
        // We need to catch all exceptions to finish handling the broadcast
        @Suppress("TooGenericExceptionCaught")
        GlobalScope.launch(dispatcher) {
            try {
                withTimeout(BROADCAST_TIMEOUT) { handleIntent(context, intent) }
            } catch (_: TimeoutCancellationException) {
                Timber.w("Failed to handle broadcast within %s", BROADCAST_TIMEOUT)
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to handle broadcast")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
