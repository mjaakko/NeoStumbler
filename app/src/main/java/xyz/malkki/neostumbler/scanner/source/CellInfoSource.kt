package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.CellTower
import kotlin.time.Duration

interface CellInfoSource {
    fun getCellInfoFlow(interval: Duration): Flow<List<CellTower>>
}