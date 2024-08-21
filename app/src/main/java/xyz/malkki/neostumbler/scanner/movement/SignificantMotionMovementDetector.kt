package xyz.malkki.neostumbler.scanner.movement

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SignificantMotionMovementDetector(private val sensorManager: SensorManager, private val notMovingDelay: Duration = 45.seconds) : MovementDetector {
    override fun getIsMovingFlow(): Flow<Boolean> = callbackFlow {
            val significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)

            val listener = object : TriggerEventListener() {
                override fun onTrigger(event: TriggerEvent) {
                    trySendBlocking(Unit)

                    sensorManager.requestTriggerSensor(this, significantMotionSensor)
                }
            }

            sensorManager.requestTriggerSensor(listener, significantMotionSensor)

            awaitClose {
                sensorManager.cancelTriggerSensor(listener, significantMotionSensor)
            }
        }
        .onStart {
            emit(Unit)
        }
        .flatMapLatest {
            flow {
                //Significant motion detected -> we are moving
                emit(true)

                delay(notMovingDelay)

                //If another significant motion is not detected within the time limit -> we are not moving
                emit(false)
            }
        }
}