package xyz.malkki.neostumbler.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import xyz.malkki.neostumbler.db.converters.InstantConverters
import xyz.malkki.neostumbler.db.converters.LocalDateConverters
import xyz.malkki.neostumbler.db.dao.BluetoothBeaconDao
import xyz.malkki.neostumbler.db.dao.CellTowerDao
import xyz.malkki.neostumbler.db.dao.ExportDao
import xyz.malkki.neostumbler.db.dao.PositionDao
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.dao.StatisticsDao
import xyz.malkki.neostumbler.db.dao.WifiAccessPointDao
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.Position
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.db.migrations.RenameTablesToEntities

@Database(
    exportSchema = true,
    version = 7,
    entities = [Report::class, Position::class, WifiAccessPointEntity::class, CellTowerEntity::class, BluetoothBeaconEntity::class,],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = RenameTablesToEntities::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ]
)
@TypeConverters(InstantConverters::class, LocalDateConverters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    abstract fun positionDao(): PositionDao

    abstract fun wifiAccessPointDao(): WifiAccessPointDao

    abstract fun cellTowerDao(): CellTowerDao

    abstract fun bluetoothBeaconDao(): BluetoothBeaconDao

    abstract fun exportDao(): ExportDao

    abstract fun statisticsDao(): StatisticsDao
}