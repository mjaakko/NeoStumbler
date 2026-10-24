package xyz.malkki.neostumbler.core

import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition

fun someReport(
    id: Long = 1,
    timestamp: Instant = Instant.now(),
    position: Position = somePosition(),
    wifiAccessPoints: List<WifiAccessPoint> = emptyList(),
    bluetoothBeacons: List<BluetoothBeacon> = emptyList(),
    cellTowers: List<CellTower> = emptyList(),
) =
    Report(
        id = id,
        timestamp = timestamp,
        uploaded = false,
        uploadTimestamp = null,
        position =
            ReportPosition(
                position = position,
                age = 1000.milliseconds,
            ),
        wifiAccessPoints =
            wifiAccessPoints.mapIndexed { index, wap ->
                ReportEmitter(id = id + index, emitter = wap, age = 1500.milliseconds)
            },
        cellTowers =
            cellTowers.mapIndexed { index, ct ->
                ReportEmitter(
                    id = id + wifiAccessPoints.size + index,
                    emitter = ct,
                    age = 1500.milliseconds,
                )
            },
        bluetoothBeacons =
            bluetoothBeacons.mapIndexed { index, bb ->
                ReportEmitter(
                    id = id + wifiAccessPoints.size + cellTowers.size + index,
                    emitter = bb,
                    age = 1500.milliseconds,
                )
            },
    )
