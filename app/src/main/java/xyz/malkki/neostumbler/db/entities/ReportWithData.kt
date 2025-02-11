package xyz.malkki.neostumbler.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ReportWithData(
    @Embedded val report: Report,
    @Relation(parentColumn = "id", entityColumn = "reportId") val positionEntity: PositionEntity,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val wifiAccessPointEntities: List<WifiAccessPointEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val cellTowerEntities: List<CellTowerEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val bluetoothBeaconEntities: List<BluetoothBeaconEntity>,
)
