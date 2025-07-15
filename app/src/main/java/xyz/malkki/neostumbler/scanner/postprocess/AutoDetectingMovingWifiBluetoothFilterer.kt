package xyz.malkki.neostumbler.scanner.postprocess

import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.scanner.data.ReportData

private const val MAX_DISTANCE_FROM_EXISTING = 500.0

class AutoDetectingMovingWifiBluetoothFilterer(
    private val maxDistanceFromExisting: Double = MAX_DISTANCE_FROM_EXISTING
) : ReportPostProcessor {
    private val wifiLocations = mutableMapOf<String, LatLng>()
    private val bluetoothLocations = mutableMapOf<String, LatLng>()

    private val movingWifis = mutableSetOf<String>()
    private val movingBluetooths = mutableSetOf<String>()

    override fun postProcessReport(reportData: ReportData): ReportData? {
        movingWifis +=
            reportData.wifiAccessPoints
                .filter { wifi ->
                    val existingLocation =
                        wifiLocations.getOrPut(wifi.macAddress) { reportData.position.latLng }

                    existingLocation.distanceTo(reportData.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.macAddress }

        movingBluetooths +=
            reportData.bluetoothBeacons
                .filter { bluetooth ->
                    val existingLocation =
                        bluetoothLocations.getOrPut(bluetooth.macAddress) {
                            reportData.position.latLng
                        }

                    existingLocation.distanceTo(reportData.position.latLng) >=
                        maxDistanceFromExisting
                }
                .map { it.macAddress }

        return reportData.copy(
            wifiAccessPoints = reportData.wifiAccessPoints.filter { it.macAddress !in movingWifis },
            bluetoothBeacons =
                reportData.bluetoothBeacons.filter { it.macAddress !in movingBluetooths },
        )
    }
}
