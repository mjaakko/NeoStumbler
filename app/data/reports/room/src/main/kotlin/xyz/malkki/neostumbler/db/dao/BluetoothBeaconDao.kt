package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.Insert
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity

@Dao
internal interface BluetoothBeaconDao {
    @Insert suspend fun insertAll(vararg bluetoothBeaconEntities: BluetoothBeaconEntity)
}
