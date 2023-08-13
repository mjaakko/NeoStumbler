package xyz.malkki.wifiscannerformls.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.wifiscannerformls.db.entities.CellTower

@Dao
interface CellTowerDao {
    @Insert
    suspend fun insertAll(vararg cellTowers: CellTower)
}