package xyz.malkki.neostumbler.data.thermal

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

private const val OVERHEAT_THRESHOLD = PowerManager.THERMAL_STATUS_SEVERE

class PowerManagerThermalStatusProvider(private val powerManager: PowerManager) :
    ThermalStatusProvider {
    constructor(
        context: Context
    ) : this(context.applicationContext.getSystemService<PowerManager>()!!)

    override fun getIsDeviceOverheatingFlow(): Flow<Boolean> {
        return powerManager
            .getThermalStatusFlow()
            .map { thermalStatus -> thermalStatus >= OVERHEAT_THRESHOLD }
            .distinctUntilChanged()
    }
}

private fun PowerManager.getThermalStatusFlow(): Flow<Int> = callbackFlow {
    val listener = PowerManager.OnThermalStatusChangedListener {
        Timber.d("Thermal status changed: $it")

        trySendBlocking(it)
    }

    addThermalStatusListener(Dispatchers.Default.asExecutor(), listener)

    send(currentThermalStatus)

    awaitClose { removeThermalStatusListener(listener) }
}
