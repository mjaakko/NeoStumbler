package xyz.malkki.wifiscannerformls.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.wifiscannerformls.db.entities.Position

@Dao
interface PositionDao {
    @Insert
    suspend fun insert(position: Position)
}