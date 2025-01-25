package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.annotation.SuppressLint
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import timber.log.Timber
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.extensions.getServiceStateFlow
import xyz.malkki.neostumbler.utils.ImmediateExecutor
import xyz.malkki.neostumbler.utils.delayWithMinDuration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Android seems to throttle cell scans to about once every 5 seconds.
 * Trying to scan more often than that will just return the old cell tower data.
 */
private val MIN_INTERVAL = 5.seconds

private val MAX_INTERVAL = 1.minutes

class TelephonyManagerCellInfoSource(
    private val telephonyManager: TelephonyManager,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime
) : CellInfoSource {
    /**
     * On some devices, cells don't include mobile network code (https://github.com/mjaakko/NeoStumbler/issues/360#issuecomment-2563861008)
     *
     * We can try to fix this by extracting the MNC from service state
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE])
    private fun List<CellTower>.addMncIfMissing(serviceState: ServiceState?): List<CellTower> {
        if (serviceState == null || serviceState.operatorNumeric == null) {
            return this
        }

        val mobileCountryCodes = mapNotNull { it.mobileCountryCode }.toSet()
        if (mobileCountryCodes.size != 1) {
            //MCC not unique, we can't extract MNC from the service state
            return this
        } else {
            val mcc = mobileCountryCodes.first()

            if (!serviceState.operatorNumeric.startsWith(mcc)) {
                //Service state is for a different mobile country code, we can't use the MNC
                return this
            }

            val mnc = serviceState.operatorNumeric.replaceFirst(mcc, "")

            return map { cellTower ->
                if (cellTower.mobileNetworkCode == null) {
                    cellTower.copy(mobileNetworkCode = mnc)
                } else {
                    cellTower
                }
            }
        }
    }

    private fun List<CellTower>.fillMissingDataFromOtherCells(): List<CellTower> {
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

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE])
    override fun getCellInfoFlow(interval: Flow<Duration>): Flow<List<CellTower>> = callbackFlow {
        val initialServiceState = telephonyManager.serviceState

        val serviceState = telephonyManager
            .getServiceStateFlow()
            .stateIn(
                this,
                started = SharingStarted.Eagerly,
                initialValue = initialServiceState
            )

        val scanInterval = interval
            .map {
                it.coerceIn(
                    minimumValue = MIN_INTERVAL,
                    maximumValue = MAX_INTERVAL
                )
            }
            .stateIn(this, started = SharingStarted.Eagerly, initialValue = MAX_INTERVAL)

        val rendezvousQueue = Channel<Unit>(capacity = Channel.RENDEZVOUS)

        val cellInfoCallback = object: TelephonyManager.CellInfoCallback() {
            override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                val cellTowers = cellInfo
                    .mapNotNull {
                        CellTower.fromCellInfo(it)
                    }
                    .addMncIfMissing(serviceState.value)
                    .fillMissingDataFromOtherCells()
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
            val scannedAt = timeSource.invoke()

            rendezvousQueue.receive()

            delayWithMinDuration(scannedAt, timeSource, scanInterval)
        }

        awaitClose {
            rendezvousQueue.close()
        }
    }
    .distinctUntilChangedBy { cellTowers ->
        //Check the timestamp to make sure that we have received new data
        cellTowers.maxOfOrNull { cellTower ->
            cellTower.timestamp
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