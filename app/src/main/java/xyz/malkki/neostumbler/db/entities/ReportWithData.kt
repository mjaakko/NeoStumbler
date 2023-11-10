package xyz.malkki.neostumbler.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ReportWithData(
    @Embedded val report: Report,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val position: Position,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val wifiAccessPoints: List<WifiAccessPoint>,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val cellTowers: List<CellTower>,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val bluetoothBeacons: List<BluetoothBeacon>
)
