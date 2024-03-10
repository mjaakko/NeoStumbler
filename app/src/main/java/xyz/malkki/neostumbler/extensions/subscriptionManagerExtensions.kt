package xyz.malkki.neostumbler.extensions

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.concurrent.thread

@RequiresPermission(Manifest.permission.READ_PHONE_STATE)
fun SubscriptionManager.getActiveSubscriptionIds(): Flow<List<Int>> = callbackFlow {
    var looper: Looper? = null
    var listener: OnSubscriptionsChangedListener? = null

    //Separate thread has to be used here because OnSubscriptionsChangedListener needs a thread with a Looper
    thread {
        Looper.prepare()

        looper = Looper.myLooper()!!

        listener = object : OnSubscriptionsChangedListener() {
            @SuppressLint("MissingPermission")
            override fun onSubscriptionsChanged() {
                val subscriptions = this@getActiveSubscriptionIds.activeSubscriptionInfoList ?: emptyList()

                trySendBlocking(subscriptions.map { it.subscriptionId })
            }
        }

        Looper.loop()
    }

    //Busywait because it shouldn't take long to prepare the looper
    while (listener == null) {
        yield()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this@getActiveSubscriptionIds.addOnSubscriptionsChangedListener(ImmediateExecutor, listener!!)
    } else {
        this@getActiveSubscriptionIds.addOnSubscriptionsChangedListener(listener)
    }

    awaitClose {
        this@getActiveSubscriptionIds.removeOnSubscriptionsChangedListener(listener)
        looper!!.quit()
    }
}.distinctUntilChanged { old, new ->
    //Only emit new values if the IDs have changed
    HashSet(old) == HashSet(new)
}