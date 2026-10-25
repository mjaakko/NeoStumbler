package xyz.malkki.neostumbler.core.report

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.somePositionObservation

fun someReportData(
    position: PositionObservation = somePositionObservation(),
    cellTowers: List<EmitterObservation<CellTower, String>> = emptyList(),
    wifiAccessPoints: List<EmitterObservation<WifiAccessPoint, MacAddress>> = emptyList(),
    bluetoothBeacons: List<EmitterObservation<BluetoothBeacon, MacAddress>> = emptyList(),
) =
    ReportData(
        position = position,
        cellTowers = cellTowers,
        wifiAccessPoints = wifiAccessPoints,
        bluetoothBeacons = bluetoothBeacons,
    )
