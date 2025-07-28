package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.PositionEntity

@Dao
internal interface PositionDao {
    @Insert suspend fun insert(positionEntity: PositionEntity)
}
