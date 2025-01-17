package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.CellTower
import kotlin.time.Duration

fun interface CellInfoSource {
    fun getCellInfoFlow(interval: Flow<Duration>): Flow<List<CellTower>>
}