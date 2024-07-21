package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity

@Dao
interface WifiAccessPointDao {
    @Insert
    suspend fun insertAll(vararg wifiAccessPointEntity: WifiAccessPointEntity)
}