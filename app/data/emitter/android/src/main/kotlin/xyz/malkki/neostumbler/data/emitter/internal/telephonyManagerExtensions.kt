@file:Suppress("DEPRECATION") // PhoneStateListener is deprecated on Android 12+ś

package xyz.malkki.neostumbler.data.emitter.internal

import android.Manifest
import android.os.Build
import android.os.HandlerThread
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.executors.ImmediateExecutor

@RequiresPermission(Manifest.permission.READ_PHONE_STATE)
internal fun TelephonyManager.getServiceStateFlow(): Flow<ServiceState> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getServiceStateFlowS()
    } else {
        getServiceStateFlowLegacy()
    }
}

private fun TelephonyManager.getServiceStateFlowLegacy(): Flow<ServiceState> = callbackFlow {
    var listener: PhoneStateListener? = null

    val handlerThread =
        object : HandlerThread("PhoneStateHandler") {
                override fun onLooperPrepared() {
                    listener =
                        object : PhoneStateListener() {
                            override fun onServiceStateChanged(serviceState: ServiceState?) {
                                serviceState?.let { trySendBlocking(it) }
                            }
                        }

                    listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
                }
            }
            .apply { start() }

    awaitClose {
        listener?.let { listen(it, PhoneStateListener.LISTEN_NONE) }

        handlerThread.quit()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun TelephonyManager.getServiceStateFlowS(): Flow<ServiceState> = callbackFlow {
    val listener =
        object : TelephonyCallback.ServiceStateListener, TelephonyCallback() {
            override fun onServiceStateChanged(serviceState: ServiceState) {
                trySendBlocking(serviceState)
            }
        }

    registerTelephonyCallback(ImmediateExecutor, listener)

    awaitClose { unregisterTelephonyCallback(listener) }
}
