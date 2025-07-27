package xyz.malkki.neostumbler.core.report

import java.time.Instant
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint

data class Report(
    val id: Long = 0,
    val timestamp: Instant,
    val uploaded: Boolean,
    val uploadTimestamp: Instant?,
    val position: ReportPosition,
    val wifiAccessPoints: List<ReportEmitter<WifiAccessPoint, MacAddress>>,
    val bluetoothBeacons: List<ReportEmitter<BluetoothBeacon, MacAddress>>,
    val cellTowers: List<ReportEmitter<CellTower, String>>,
)
