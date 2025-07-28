package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.annotation.SuppressLint
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.CellTower.Companion.fillMissingData
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.delayWithMinDuration
import xyz.malkki.neostumbler.data.emitter.internal.getServiceStateFlow
import xyz.malkki.neostumbler.data.emitter.mapper.toCellTower
import xyz.malkki.neostumbler.executors.ImmediateExecutor

/**
 * Android seems to throttle cell scans to about once every 5 seconds. Trying to scan more often
 * than that will just return the old cell tower data.
 */
private val MIN_INTERVAL = 5.seconds

private val MAX_INTERVAL = 1.minutes

class TelephonyManagerActiveCellInfoSource(
    private val telephonyManager: TelephonyManager,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime,
) : ActiveCellInfoSource {
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    override fun getCellInfoFlow(
        interval: Flow<Duration>
    ): Flow<List<EmitterObservation<CellTower, String>>> =
        callbackFlow {
                val initialServiceState = telephonyManager.serviceState

                /**
                 * On some devices, cells don't include mobile network code
                 * (https://github.com/mjaakko/NeoStumbler/issues/360#issuecomment-2563861008)
                 *
                 * We can try to fix this by extracting the MNC from service state
                 */
                val serviceState =
                    telephonyManager
                        .getServiceStateFlow()
                        .stateIn(
                            this,
                            started = SharingStarted.Eagerly,
                            initialValue = initialServiceState,
                        )

                val scanInterval =
                    interval
                        .map {
                            it.coerceIn(minimumValue = MIN_INTERVAL, maximumValue = MAX_INTERVAL)
                        }
                        .stateIn(
                            this,
                            started = SharingStarted.Eagerly,
                            initialValue = MAX_INTERVAL,
                        )

                val rendezvousQueue = Channel<Unit>(capacity = Channel.RENDEZVOUS)

                val cellInfoCallback =
                    object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                            val cellTowers =
                                cellInfo
                                    .mapNotNull { it.toCellTower() }
                                    .fillMissingData(serviceState.value?.operatorNumeric)
                                    // Filter cell infos which don't have enough useful data to be
                                    // collected
                                    .filter { it.emitter.hasEnoughData() }

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

                awaitClose { rendezvousQueue.close() }
            }
            .distinctUntilChangedBy { cellTowers ->
                // Check the timestamp to make sure that we have received new data
                cellTowers.maxOfOrNull { cellTower -> cellTower.timestamp }
            }
}
