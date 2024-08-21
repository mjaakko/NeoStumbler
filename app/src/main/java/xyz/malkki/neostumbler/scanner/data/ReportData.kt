package xyz.malkki.neostumbler.scanner.data

import xyz.malkki.neostumbler.common.LocationWithSource
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.WifiAccessPoint

/**
 * Container for the data included in a single report
 */
data class ReportData(
    val location: LocationWithSource,
    val cellTowers: List<CellTower>,
    val wifiAccessPoints: List<WifiAccessPoint>,
    val bluetoothBeacons: List<BluetoothBeacon>
)
