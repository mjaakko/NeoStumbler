package xyz.malkki.neostumbler.data.emitter

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation

/** API for actively scanning cell towers */
fun interface ActiveCellInfoSource {
    fun getCellInfoFlow(interval: Flow<Duration>): Flow<List<EmitterObservation<CellTower, String>>>
}
