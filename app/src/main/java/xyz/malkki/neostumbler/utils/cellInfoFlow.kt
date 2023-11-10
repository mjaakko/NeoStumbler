package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CellInfoCallback
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.time.Duration

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun getCellInfoFlow(context: Context, scanInterval: Duration): Flow<List<CellInfo>> = channelFlow {
    val rendezvousQueue = Channel<Unit>(capacity = Channel.RENDEZVOUS)

    val telephonyManager = context.getSystemService<TelephonyManager>()!!

    val cellInfoCallback = object: CellInfoCallback() {
        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
            trySendBlocking(cellInfo)

            rendezvousQueue.trySendBlocking(Unit)
        }

        override fun onError(errorCode: Int, detail: Throwable?) {
            Timber.w(detail, "Cell info update failed, error code: $errorCode")

            rendezvousQueue.trySendBlocking(Unit)
        }
    }

    while (isActive) {
        telephonyManager.requestCellInfoUpdate(ImmediateExecutor, cellInfoCallback)

        rendezvousQueue.receive()

        delay(scanInterval)
    }

    awaitClose {
        rendezvousQueue.close()
    }
}