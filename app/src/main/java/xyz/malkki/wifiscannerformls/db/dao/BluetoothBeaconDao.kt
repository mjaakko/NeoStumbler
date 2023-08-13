package xyz.malkki.wifiscannerformls.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.wifiscannerformls.db.entities.BluetoothBeacon

@Dao
interface BluetoothBeaconDao {
    @Insert
    suspend fun insertAll(vararg bluetoothBeacons: BluetoothBeacon)
}