package xyz.malkki.neostumbler.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import xyz.malkki.neostumbler.db.entities.WifiAccessPoint

@Dao
interface WifiAccessPointDao {
    @Insert
    suspend fun insertAll(vararg wifiAccessPoint: WifiAccessPoint)

    @Query("""
        SELECT COUNT(DISTINCT macAddress) FROM WifiAccessPoint
    """)
    fun countDistinct(): LiveData<Long>
}