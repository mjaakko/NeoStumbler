package xyz.malkki.neostumbler.location

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.common.LocationWithSource
import kotlin.time.Duration

fun interface LocationSource {
    fun getLocations(interval: Duration): Flow<LocationWithSource>
}