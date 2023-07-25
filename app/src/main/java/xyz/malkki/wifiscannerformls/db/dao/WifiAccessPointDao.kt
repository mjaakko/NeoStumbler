package xyz.malkki.wifiscannerformls.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.wifiscannerformls.db.entities.WifiAccessPoint

@Dao
interface WifiAccessPointDao {
    @Insert
    suspend fun insertAll(vararg wifiAccessPoint: WifiAccessPoint)
}