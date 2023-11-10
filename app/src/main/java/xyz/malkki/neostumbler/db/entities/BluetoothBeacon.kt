package xyz.malkki.neostumbler.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.altbeacon.beacon.Beacon
import java.time.Instant

@Entity(foreignKeys = [ForeignKey(entity = Report::class, parentColumns = ["id"], childColumns = ["reportId"], onDelete = ForeignKey.CASCADE)])
data class BluetoothBeacon(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val macAddress: String,
    val age: Long,
    val name: String?,
    val signalStrength: Int?,
    @ColumnInfo(index = true) val reportId: Long?
) {
    companion object {
        fun fromBeacon(reportId: Long, currentTime: Instant, beacon: Beacon): BluetoothBeacon {
            return BluetoothBeacon(
                null,
                beacon.bluetoothAddress,
                currentTime.toEpochMilli() - beacon.lastCycleDetectionTimestamp,
                null,
                beacon.rssi,
                reportId
            )
        }
    }
}