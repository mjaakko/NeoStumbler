package xyz.malkki.neostumbler.db.migrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable.Entries(
    RenameTable(fromTableName = "WifiAccessPoint", toTableName = "WifiAccessPointEntity"),
    RenameTable(fromTableName = "CellTower", toTableName = "CellTowerEntity"),
    RenameTable(fromTableName = "BluetoothBeacon", toTableName = "BluetoothBeaconEntity"),
)
internal class RenameTablesToEntities : AutoMigrationSpec
