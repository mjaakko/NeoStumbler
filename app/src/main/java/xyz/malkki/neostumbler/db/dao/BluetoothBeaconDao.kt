package xyz.malkki.neostumbler.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import xyz.malkki.neostumbler.db.entities.BluetoothBeacon

@Dao
interface BluetoothBeaconDao {
    @Insert
    suspend fun insertAll(vararg bluetoothBeacons: BluetoothBeacon)

    @Query("""
        SELECT COUNT(DISTINCT macAddress) FROM BluetoothBeacon
    """)
    fun countDistinct(): LiveData<Long>
}