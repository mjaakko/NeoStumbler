package xyz.malkki.neostumbler.core.report

import java.time.Instant

data class ReportWithStats(
    val reportId: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val wifiAccessPointCount: Int,
    val cellTowerCount: Int,
    val bluetoothBeaconCount: Int,
)
