package xyz.malkki.neostumbler.db.entities

import androidx.room.TypeConverters
import java.time.Instant
import xyz.malkki.neostumbler.roomconverters.InstantConverters

@TypeConverters(InstantConverters::class)
data class ReportWithStats(
    val reportId: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val wifiAccessPointCount: Int,
    val cellTowerCount: Int,
    val bluetoothBeaconCount: Int,
)
