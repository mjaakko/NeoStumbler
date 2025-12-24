package xyz.malkki.neostumbler.data.emitter.internal

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import xyz.malkki.neostumbler.broadcastreceiverflow.broadcastReceiverFlow

internal fun Context.getDeviceInteractiveFlow(): Flow<Boolean> {
    val powerManager = getSystemService<PowerManager>()!!

    return broadcastReceiverFlow(
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
        .map { powerManager.isInteractive }
        .onStart { emit(powerManager.isInteractive) }
        .distinctUntilChanged()
}
