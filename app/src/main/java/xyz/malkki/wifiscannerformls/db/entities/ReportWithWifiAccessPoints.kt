package xyz.malkki.wifiscannerformls.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class ReportWithWifiAccessPoints(
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
    val wifiAccessPoints: List<WifiAccessPoint>
)
