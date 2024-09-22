package xyz.malkki.neostumbler.scanner.source

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import xyz.malkki.neostumbler.domain.AirPressureObservation
import xyz.malkki.neostumbler.extensions.throttleLast
import kotlin.time.Duration

class PressureSensorAirPressureSource(private val sensorManager: SensorManager) : AirPressureSource {
    override fun getAirPressureFlow(interval: Duration): Flow<AirPressureObservation> =
        callbackFlow {
            val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySendBlocking(AirPressureObservation(airPressure = event.values[0], timestamp = event.timestamp / 1_000_000))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

                }
            }

            sensorManager.registerListener(listener, pressureSensor, interval.inWholeMicroseconds.toInt())

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
        //SensorManager can send data more often than requested -> throttle the flow
        .throttleLast(interval)
}