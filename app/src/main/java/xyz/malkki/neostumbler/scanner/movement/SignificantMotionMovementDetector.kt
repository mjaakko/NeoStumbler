package xyz.malkki.neostumbler.scanner.movement

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.location.Location
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//Minimum distance moved to consider the device to be moving
//If the location has not changed by at least this amount, listening for locations is stopped
private const val DISTANCE_THRESHOLD = 200

/**
 * Movement detector which uses [Sensor.TYPE_SIGNIFICANT_MOTION] to detect movement.
 * The movement detector checks whether the location is changing and if not pauses listening for locations until the significant motion sensor is triggered
 *
 * @property locationSource Location source used for checking movement
 */
class SignificantMotionMovementDetector(
    private val sensorManager: SensorManager,
    private val notMovingDelay: Duration = 30.seconds,
    private val locationSource: () -> Flow<Location>
) : MovementDetector {
    private val significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)!!

    private suspend fun waitForSignificantMotion() = suspendCancellableCoroutine { continuation ->
        val listener = object : TriggerEventListener() {
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

    override fun getIsMovingFlow(): Flow<Boolean> = getSignificantMotionFlow()
        .flatMapLatest {
            flow {
                //Significant motion detected -> we are moving
                emit(true)

                locationSource.invoke()
                    //Emit values only when the location changes significantly
                    .distinctUntilChanged { a, b -> a.distanceTo(b) <= DISTANCE_THRESHOLD }
                    .map {}
                    //Complete the flow if no value was emitted within the limit
                    .timeout(notMovingDelay)
                    .catch { ex ->
                        if (ex !is TimeoutCancellationException) {
                            throw ex
                        }
                    }
                    .collect()

                //If the location has not changed and there hasn't been another significant motion -> we are not moving
                emit(false)
            }
        }
        .distinctUntilChanged()
}