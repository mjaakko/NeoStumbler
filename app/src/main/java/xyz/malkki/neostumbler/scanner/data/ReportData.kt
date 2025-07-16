package xyz.malkki.neostumbler.scanner.data

import xyz.malkki.neostumbler.core.BluetoothBeacon
import xyz.malkki.neostumbler.core.CellTower
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.WifiAccessPoint

/** Container for the data included in a single report */
data class ReportData(
    val position: Position,
    val cellTowers: List<CellTower>,
    val wifiAccessPoints: List<WifiAccessPoint>,
    val bluetoothBeacons: List<BluetoothBeacon>,
) {
    /** true if the report does not contain any data */
    val isEmpty: Boolean
        get() = cellTowers.isEmpty() && wifiAccessPoints.isEmpty() && bluetoothBeacons.isEmpty()
}
