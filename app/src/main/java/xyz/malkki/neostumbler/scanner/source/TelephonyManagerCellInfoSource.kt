package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import timber.log.Timber
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.time.Duration

class TelephonyManagerCellInfoSource(private val telephonyManager: TelephonyManager) : CellInfoSource {
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getCellInfoFlow(interval: Duration): Flow<List<CellInfo>> = callbackFlow {
        val rendezvousQueue = Channel<Unit>(capacity = Channel.RENDEZVOUS)

        val cellInfoCallback = object: TelephonyManager.CellInfoCallback() {
            override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                //Filter cell infos which don't have enough useful data to be collected
                trySendBlocking(cellInfo.filter { it.hasEnoughData() })

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

            delay(interval)
        }

        awaitClose {
            rendezvousQueue.close()
        }
    }
}

/**
 * Checks if the cell info has enough useful data. Used for filtering neighbouring cells which don't specify their cell ID etc.
 */
private fun CellInfo.hasEnoughData(): Boolean {
    return when (this) {
        is CellInfoGsm -> {
            cellIdentity.mccString != null && cellIdentity.mncString != null &&
                    (cellIdentity.cid != CellInfo.UNAVAILABLE || cellIdentity.lac!= CellInfo.UNAVAILABLE)
        }
        is CellInfoWcdma -> {
            cellIdentity.mccString != null && cellIdentity.mncString != null &&
                    (cellIdentity.cid != CellInfo.UNAVAILABLE || cellIdentity.lac!= CellInfo.UNAVAILABLE || cellIdentity.psc != CellInfo.UNAVAILABLE)
        }
        is CellInfoLte -> {
            cellIdentity.mccString != null && cellIdentity.mncString != null &&
                    (cellIdentity.ci != CellInfo.UNAVAILABLE || cellIdentity.tac != CellInfo.UNAVAILABLE || cellIdentity.pci != CellInfo.UNAVAILABLE)
        }
        is CellInfoNr -> {
            val cellIdentity = cellIdentity as CellIdentityNr

            cellIdentity.mccString != null && cellIdentity.mncString != null &&
                    (cellIdentity.nci != CellInfo.UNAVAILABLE_LONG || cellIdentity.tac != CellInfo.UNAVAILABLE || cellIdentity.pci != CellInfo.UNAVAILABLE)
        }
        else -> false
    }
}