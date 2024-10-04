package xyz.malkki.neostumbler.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.HandlerThread
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import xyz.malkki.neostumbler.utils.ImmediateExecutor

@RequiresPermission(Manifest.permission.READ_PHONE_STATE)
fun SubscriptionManager.getActiveSubscriptionIds(): Flow<List<Int>> = callbackFlow {
    var listener: OnSubscriptionsChangedListener? = null

    val handlerThread = object : HandlerThread("ActiveSubscriptionHandler") {
            override fun onLooperPrepared() {
                listener = object : OnSubscriptionsChangedListener() {
                    @SuppressLint("MissingPermission")
                    override fun onSubscriptionsChanged() {
                        val subscriptions = this@getActiveSubscriptionIds.activeSubscriptionInfoList ?: emptyList()

                        trySendBlocking(subscriptions.map { it.subscriptionId })
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    this@getActiveSubscriptionIds.addOnSubscriptionsChangedListener(ImmediateExecutor, listener)
                } else {
                    @Suppress("DEPRECATION")
                    this@getActiveSubscriptionIds.addOnSubscriptionsChangedListener(listener)
                }
            }
        }
        .apply {
            start()
        }

    awaitClose {
        this@getActiveSubscriptionIds.removeOnSubscriptionsChangedListener(listener)

        handlerThread.quit()
    }
}.distinctUntilChanged { old, new ->
    //Only emit new values if the IDs have changed
    HashSet(old) == HashSet(new)
}