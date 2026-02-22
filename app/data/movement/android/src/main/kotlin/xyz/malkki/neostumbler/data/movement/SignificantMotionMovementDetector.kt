package xyz.malkki.neostumbler.data.movement

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.malkki.neostumbler.data.location.LocationSourceProvider

// Minimum distance moved to consider the device to be moving
// If the location has not changed by at least this amount, listening for locations is stopped
private const val DISTANCE_THRESHOLD = 200

/**
 * Movement detector which uses [Sensor.TYPE_SIGNIFICANT_MOTION] to detect movement. The movement
 * detector checks whether the location is changing and if not pauses listening for locations until
 * the significant motion sensor is triggered
 *
 * @property locationSourceProvider Location source used for checking movement
 */
class SignificantMotionMovementDetector(
    private val sensorManager: SensorManager,
    private val notMovingDelay: Duration = 30.seconds,
    private val locationSourceProvider: LocationSourceProvider,
) : MovementDetector {
    private val significantMotionSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)!!

    private suspend fun waitForSignificantMotion() = suspendCancellableCoroutine { continuation ->
        val listener =
            object : TriggerEventListener() {
                override fun onTrigger(event: TriggerEvent) {
                    continuation.resume(Unit)
                }
            }

        sensorManager.requestTriggerSensor(listener, significantMotionSensor)

        continuation.invokeOnCancellation {
            sensorManager.cancelTriggerSensor(listener, significantMotionSensor)
        }
    }

    private fun getSignificantMotionFlow(): Flow<Unit> = flow {
        emit(Unit)

        while (true) {
            waitForSignificantMotion()

            emit(Unit)
        }
    }

    private fun getSignificantMotionIsMovingFlow(): Flow<Boolean> {
        return getSignificantMotionFlow().transformLatest {
            emit(true)

            delay(notMovingDelay)

            emit(false)
        }
    }

    private fun getLocationBasedIsMovingFlow(): Flow<Boolean> {
        return flow { emit(locationSourceProvider.getLocationSource()) }
            .flatMapLatest { it.getLocations(interval = 1.seconds, usePassiveProvider = true) }
            // Emit values only when the location changes significantly
            .distinctUntilChanged { a, b ->
                a.position.latLng.distanceTo(b.position.latLng) <= DISTANCE_THRESHOLD
            }
            .map {}
            .transformLatest {
                emit(true)

                delay(notMovingDelay)

                emit(false)
            }
    }

    override fun getIsMovingFlow(): Flow<Boolean> {
        return combine(getSignificantMotionIsMovingFlow(), getLocationBasedIsMovingFlow()) { a, b ->
                a || b
            }
            .onStart { emit(true) }
            .distinctUntilChanged()
    }
}
