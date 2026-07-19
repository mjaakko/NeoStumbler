package xyz.malkki.neostumbler.core.report

import kotlin.time.Duration
import xyz.malkki.neostumbler.core.Position

data class ReportPosition(
    val position: Position,
    /** Age of the position relative to the report timestamp */
    val age: Duration,
)
