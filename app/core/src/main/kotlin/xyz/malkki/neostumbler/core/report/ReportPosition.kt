package xyz.malkki.neostumbler.core.report

import xyz.malkki.neostumbler.core.Position

data class ReportPosition(
    val position: Position,
    /** Age of the position relative to the report timestamp */
    val age: Long,
)
