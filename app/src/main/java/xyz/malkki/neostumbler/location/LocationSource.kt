package xyz.malkki.neostumbler.location

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.Position
import kotlin.time.Duration

fun interface LocationSource {
    fun getLocations(interval: Duration): Flow<Position>
}