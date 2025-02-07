package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.domain.LatLng

@Dao
interface PositionDao {
    @Insert suspend fun insert(positionEntity: PositionEntity)

    /**
     * Gets coordinates of the latest position as a pair
     *
     * @return Pair of coordinates (latitude, longitude)
     */
    @Transaction
    @Query(
        "SELECT p.latitude AS latitude, p.longitude AS longitude FROM PositionEntity p JOIN Report r ON p.reportId = r.id ORDER BY r.timestamp DESC LIMIT 1"
    )
    suspend fun getLatestPosition(): LatLng?
}
