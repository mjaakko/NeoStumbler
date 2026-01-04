package xyz.malkki.neostumbler.data.airpressure

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PressureSensorAirPressureSourceTest {
    @Test
    fun `Test that air pressure source does not emit values more often than requested`() = runTest {
        val mockSensorEvent = mock<SensorEvent>()
        val valuesField = mockSensorEvent::class.java.fields.find { it.name == "values" }!!
        valuesField.isAccessible = true
        valuesField.set(mockSensorEvent, floatArrayOf(1013.25f))

        val mockSensor = mock<Sensor>()

        val mockSensorManager =
            mock<SensorManager> {
                on { getDefaultSensor(any()) } doReturn mockSensor

                on { unregisterListener(any<SensorEventListener>()) } doAnswer {}

                on {
                    registerListener(any<SensorEventListener>(), any<Sensor>(), anyInt())
                } doAnswer
                    { invocation ->
                        val sensorEventListener = invocation.arguments[0] as SensorEventListener
                        backgroundScope.launch {
                            while (true) {
                                sensorEventListener.onSensorChanged(mockSensorEvent)

                                delay(100)
                            }
                        }

                        true
                    }
            }

        val airPressureSource = PressureSensorAirPressureSource(mockSensorManager)

        val elapsedTime =
            testTimeSource.measureTime {
                airPressureSource.getAirPressureFlow(1.seconds).take(10).collect()
            }

        assertFalse(elapsedTime < 10.seconds)
    }
}
