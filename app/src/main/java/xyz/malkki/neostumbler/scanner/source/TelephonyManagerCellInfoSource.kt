package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.telephony.CellInfo
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
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import kotlin.time.Duration

class TelephonyManagerCellInfoSource(private val telephonyManager: TelephonyManager) : CellInfoSource {
    private fun List<CellTower>.fillMissingData(): List<CellTower> {
        if (size == 1) {
            return this
        } else {
            val mobileCountryCodes = mapNotNull { it.mobileCountryCode }.toSet()
            val mobileNetworkCodes = mapNotNull { it.mobileNetworkCode }.toSet()

            return if (mobileCountryCodes.size != 1 || mobileNetworkCodes.size != 1) {
                this
            } else {
                map { cellTower ->
                    cellTower.copy(
                        mobileCountryCode = mobileCountryCodes.first(),
                        mobileNetworkCode = mobileNetworkCodes.first()
                    )
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun getCellInfoFlow(interval: Duration): Flow<List<CellTower>> = callbackFlow {
        val rendezvousQueue = Channel<Unit>(capacity = Channel.RENDEZVOUS)

        val cellInfoCallback = object: TelephonyManager.CellInfoCallback() {
            override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                val cellTowers = cellInfo
                    .mapNotNull {
                        CellTower.fromCellInfo(it)
                    }
                    .fillMissingData()
                    //Filter cell infos which don't have enough useful data to be collected
                    .filter { it.hasEnoughData() }

                trySendBlocking(cellTowers)

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
private fun CellTower.hasEnoughData(): Boolean {
    if (mobileCountryCode == null || mobileNetworkCode == null) {
        return false
    }

    return when (radioType) {
        CellTower.RadioType.GSM -> cellId != null || locationAreaCode != null
        CellTower.RadioType.WCDMA,
        CellTower.RadioType.LTE,
        CellTower.RadioType.NR -> cellId != null || locationAreaCode != null || primaryScramblingCode != null
    }
}