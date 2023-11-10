package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.Position

@Dao
interface PositionDao {
    @Insert
    suspend fun insert(position: Position)
}