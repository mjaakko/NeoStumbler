package xyz.malkki.neostumbler.data.location

import kotlinx.coroutines.flow.Flow

data class GpsStatus(val satellitesUsedInFix: Int, val satellitesTotal: Int)

interface GpsStatusSource {
    fun getGpsStatusFlow(): Flow<GpsStatus?>

    fun isGpsAvailable(): Flow<Boolean>
}
