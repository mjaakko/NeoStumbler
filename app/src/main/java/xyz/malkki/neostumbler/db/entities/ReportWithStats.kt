package xyz.malkki.neostumbler.db.entities

import androidx.room.TypeConverters
import xyz.malkki.neostumbler.db.converters.InstantConverters
import java.time.Instant

@TypeConverters(InstantConverters::class)
data class ReportWithStats(
    val reportId: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val wifiAccessPointCount: Int,
    val cellTowerCount: Int,
    val bluetoothBeaconCount: Int
)
