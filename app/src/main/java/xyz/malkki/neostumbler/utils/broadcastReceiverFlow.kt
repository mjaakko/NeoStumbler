package xyz.malkki.neostumbler.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive

fun Context.broadcastReceiverFlow(intentFilter: IntentFilter): Flow<Intent> = callbackFlow<Intent> {
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isActive) {
                trySendBlocking(intent)
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        registerReceiver(broadcastReceiver, intentFilter)
    }

    awaitClose {
        unregisterReceiver(broadcastReceiver)
    }
}