package xyz.malkki.neostumbler.data.location

import kotlinx.coroutines.flow.Flow

data class GpsStatus(val satellitesUsedInFix: Int, val satellitesTotal: Int)

fun interface GpsStatusSource {
    fun getGpsStatusFlow(): Flow<GpsStatus?>
}
