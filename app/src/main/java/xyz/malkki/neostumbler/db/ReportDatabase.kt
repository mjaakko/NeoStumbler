package xyz.malkki.neostumbler.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import xyz.malkki.neostumbler.db.converters.InstantConverters
import xyz.malkki.neostumbler.db.dao.BluetoothBeaconDao
import xyz.malkki.neostumbler.db.dao.CellTowerDao
import xyz.malkki.neostumbler.db.dao.ExportDao
import xyz.malkki.neostumbler.db.dao.PositionDao
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.dao.WifiAccessPointDao
import xyz.malkki.neostumbler.db.entities.BluetoothBeacon
import xyz.malkki.neostumbler.db.entities.CellTower
import xyz.malkki.neostumbler.db.entities.Position
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPoint

@Database(
    exportSchema = true,
    version = 2,
    entities = [Report::class, Position::class, WifiAccessPoint::class, CellTower::class, BluetoothBeacon::class,],
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(InstantConverters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    abstract fun positionDao(): PositionDao

    abstract fun wifiAccessPointDao(): WifiAccessPointDao

    abstract fun cellTowerDao(): CellTowerDao

    abstract fun bluetoothBeaconDao(): BluetoothBeaconDao

    abstract fun exportDao(): ExportDao
}