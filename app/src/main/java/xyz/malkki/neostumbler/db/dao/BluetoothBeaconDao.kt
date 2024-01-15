package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.BluetoothBeacon

@Dao
interface BluetoothBeaconDao {
    @Insert
    suspend fun insertAll(vararg bluetoothBeacons: BluetoothBeacon)
}