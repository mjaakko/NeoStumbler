package xyz.malkki.neostumbler.db.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "BluetoothBeaconEntity", columnName = "name"),
    DeleteColumn(tableName = "WifiAccessPointEntity", columnName = "signalToNoiseRatio"),
)
class DropUnusedColumns : AutoMigrationSpec
