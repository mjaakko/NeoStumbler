package xyz.malkki.neostumbler.core.report

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation

/** Container for the data included in a single report */
data class ReportData(
    val position: PositionObservation,
    val cellTowers: List<EmitterObservation<CellTower, String>>,
    val wifiAccessPoints: List<EmitterObservation<WifiAccessPoint, MacAddress>>,
    val bluetoothBeacons: List<EmitterObservation<BluetoothBeacon, MacAddress>>,
) {
    /** true if the report does not contain any data */
    val isEmpty: Boolean
        get() = cellTowers.isEmpty() && wifiAccessPoints.isEmpty() && bluetoothBeacons.isEmpty()
}
