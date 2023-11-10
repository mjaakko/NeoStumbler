package xyz.malkki.neostumbler.db.entities

import java.time.Instant

data class ReportWithLocation(
    val id: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double
)
