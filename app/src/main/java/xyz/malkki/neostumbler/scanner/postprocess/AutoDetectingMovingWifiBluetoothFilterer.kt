package xyz.malkki.neostumbler.scanner.postprocess

import androidx.collection.mutableLongObjectMapOf
import androidx.collection.mutableLongSetOf
import kotlin.random.Random
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.geography.LatLng

private const val MAX_DISTANCE_FROM_EXISTING = 500.0

// 1 in 100 chance to not filter the report
private const val NO_FILTER_PROBABILITY_COUNT = 100

/**
 * @param deterministic If `true`, moving devices are always filtered. If `false`, there is a 1%
 *   chance that moving devices won't be filtered. The idea is to make sure that the server has
 *   enough data to learn that the device is moving
 */
class AutoDetectingMovingWifiBluetoothFilterer(
    private val maxDistanceFromExisting: Double = MAX_DISTANCE_FROM_EXISTING,
    private val deterministic: Boolean = true,
) : ReportPostProcessor {
    private val wifiLocations = mutableLongObjectMapOf<LatLng>()
    private val bluetoothLocations = mutableLongObjectMapOf<LatLng>()

    private val movingWifis = mutableLongSetOf()
    private val movingBluetooths = mutableLongSetOf()

    override fun postProcessReport(reportData: ReportData): ReportData? {
        movingWifis +=
            reportData.wifiAccessPoints
                .filter { wifi ->
                    val existingLocation =
                        wifiLocations.getOrPut(wifi.emitter.macAddress.raw) {
                            reportData.position.position.latLng
                        }

                    existingLocation.distanceTo(reportData.position.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.emitter.macAddress.raw }
                .toLongArray()

        movingBluetooths +=
            reportData.bluetoothBeacons
                .filter { bluetooth ->
                    val existingLocation =
                        bluetoothLocations.getOrPut(bluetooth.emitter.macAddress.raw) {
                            reportData.position.position.latLng
                        }

                    existingLocation.distanceTo(reportData.position.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.emitter.macAddress.raw }
                .toLongArray()

        if (!deterministic && Random.nextInt(0, NO_FILTER_PROBABILITY_COUNT) == 0) {
            return reportData
        }

        return reportData.copy(
            wifiAccessPoints =
                reportData.wifiAccessPoints.filter { it.emitter.macAddress.raw !in movingWifis },
            bluetoothBeacons =
                reportData.bluetoothBeacons.filter {
                    it.emitter.macAddress.raw !in movingBluetooths
                },
        )
    }
}
