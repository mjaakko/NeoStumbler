package xyz.malkki.neostumbler.scanner.postprocess

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.geography.LatLng

private const val MAX_DISTANCE_FROM_EXISTING = 500.0

class AutoDetectingMovingWifiBluetoothFilterer(
    private val maxDistanceFromExisting: Double = MAX_DISTANCE_FROM_EXISTING
) : ReportPostProcessor {
    private val wifiLocations = mutableMapOf<MacAddress, LatLng>()
    private val bluetoothLocations = mutableMapOf<MacAddress, LatLng>()

    private val movingWifis = mutableSetOf<MacAddress>()
    private val movingBluetooths = mutableSetOf<MacAddress>()

    override fun postProcessReport(reportData: ReportData): ReportData? {
        movingWifis +=
            reportData.wifiAccessPoints
                .filter { wifi ->
                    val existingLocation =
                        wifiLocations.getOrPut(wifi.emitter.macAddress) {
                            reportData.position.position.latLng
                        }

                    existingLocation.distanceTo(reportData.position.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.emitter.macAddress }

        movingBluetooths +=
            reportData.bluetoothBeacons
                .filter { bluetooth ->
                    val existingLocation =
                        bluetoothLocations.getOrPut(bluetooth.emitter.macAddress) {
                            reportData.position.position.latLng
                        }

                    existingLocation.distanceTo(reportData.position.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.emitter.macAddress }

        return reportData.copy(
            wifiAccessPoints =
                reportData.wifiAccessPoints.filter { it.emitter.macAddress !in movingWifis },
            bluetoothBeacons =
                reportData.bluetoothBeacons.filter { it.emitter.macAddress !in movingBluetooths },
        )
    }
}
