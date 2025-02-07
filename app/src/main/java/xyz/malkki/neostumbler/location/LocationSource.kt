package xyz.malkki.neostumbler.location

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.Position

fun interface LocationSource {
    fun getLocations(interval: Duration): Flow<Position>
}
