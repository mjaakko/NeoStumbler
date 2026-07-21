package xyz.malkki.neostumbler.core.report

import java.time.Instant

data class ReportWithLocation(
    val id: Long,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
)
