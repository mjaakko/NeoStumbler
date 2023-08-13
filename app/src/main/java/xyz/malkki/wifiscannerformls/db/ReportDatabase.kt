package xyz.malkki.wifiscannerformls.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import xyz.malkki.wifiscannerformls.db.converters.InstantConverters
import xyz.malkki.wifiscannerformls.db.dao.BluetoothBeaconDao
import xyz.malkki.wifiscannerformls.db.dao.CellTowerDao
import xyz.malkki.wifiscannerformls.db.dao.PositionDao
import xyz.malkki.wifiscannerformls.db.dao.ReportDao
import xyz.malkki.wifiscannerformls.db.dao.WifiAccessPointDao
import xyz.malkki.wifiscannerformls.db.entities.BluetoothBeacon
import xyz.malkki.wifiscannerformls.db.entities.CellTower
import xyz.malkki.wifiscannerformls.db.entities.Position
import xyz.malkki.wifiscannerformls.db.entities.Report
import xyz.malkki.wifiscannerformls.db.entities.WifiAccessPoint

@Database(exportSchema = true, version = 1, entities = [Report::class, Position::class, WifiAccessPoint::class, CellTower::class, BluetoothBeacon::class,])
@TypeConverters(InstantConverters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun positionDao(): PositionDao
    abstract fun wifiAccessPointDao(): WifiAccessPointDao

    abstract fun cellTowerDao(): CellTowerDao

    abstract fun bluetoothBeaconDao(): BluetoothBeaconDao
}