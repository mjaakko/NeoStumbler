package xyz.malkki.neostumbler.core.report

import xyz.malkki.neostumbler.core.emitter.Emitter

data class ReportEmitter<E : Emitter<K>, K>(
    val id: Long,
    val emitter: E,
    /** Age of the emitter observation relative to the report timestamp in milliseconds */
    val age: Long,
)
