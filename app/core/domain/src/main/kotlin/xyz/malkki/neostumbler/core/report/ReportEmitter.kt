package xyz.malkki.neostumbler.core.report

import kotlin.time.Duration
import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.ranging.EstimatedDistance

data class ReportEmitter<E : Emitter<K>, K>(
    val id: Long,
    val emitter: E,
    /** Age of the emitter observation relative to the report timestamp */
    val age: Duration,
    /** Estimated distance to the emitter. `null` if unknown */
    val estimatedDistance: EstimatedDistance? = null,
)
