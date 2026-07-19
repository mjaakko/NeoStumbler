package xyz.malkki.neostumbler.core.report

import kotlin.time.Duration
import xyz.malkki.neostumbler.core.emitter.Emitter

data class ReportEmitter<E : Emitter<K>, K>(
    val id: Long,
    val emitter: E,
    /** Age of the emitter observation relative to the report timestamp */
    val age: Duration,
)
