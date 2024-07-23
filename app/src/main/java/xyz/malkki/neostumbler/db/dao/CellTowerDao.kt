package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.CellTowerEntity

@Dao
interface CellTowerDao {
    @Insert
    suspend fun insertAll(vararg cellTowerEntities: CellTowerEntity)


}