package xyz.malkki.neostumbler.scanner.postprocess

import androidx.collection.mutableLongObjectMapOf
import androidx.collection.mutableLongSetOf
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.geography.LatLng

private const val MAX_DISTANCE_FROM_EXISTING = 500.0

class AutoDetectingMovingWifiBluetoothFilterer(
    private val maxDistanceFromExisting: Double = MAX_DISTANCE_FROM_EXISTING
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
