package xyz.malkki.neostumbler.data.airpressure

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.time.Duration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.sample
import xyz.malkki.neostumbler.core.airpressure.AirPressureObservation

class PressureSensorAirPressureSource(private val sensorManager: SensorManager) :
    AirPressureSource {
    override fun getAirPressureFlow(interval: Duration): Flow<AirPressureObservation> =
        callbackFlow {
                val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

                val listener =
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            trySendBlocking(
                                AirPressureObservation(
                                    airPressure = event.values[0],
                                    timestamp = event.timestamp / 1_000_000,
                                )
                            )
                        }

                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                    }

                sensorManager.registerListener(
                    listener,
                    pressureSensor,
                    interval.inWholeMicroseconds.toInt(),
                )

                awaitClose { sensorManager.unregisterListener(listener) }
            }
            // SensorManager can send data more often than requested -> throttle the flow
            .sample(interval)
}
