package xyz.malkki.neostumbler.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import xyz.malkki.neostumbler.db.entities.CellTower

@Dao
interface CellTowerDao {
    @Insert
    suspend fun insertAll(vararg cellTowers: CellTower)


}