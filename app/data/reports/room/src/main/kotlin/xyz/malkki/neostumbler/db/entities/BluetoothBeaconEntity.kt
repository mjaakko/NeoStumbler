package xyz.malkki.neostumbler.db.entities

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation

@Entity(
    foreignKeys =
        [
            ForeignKey(
                entity = Report::class,
                parentColumns = ["id"],
                childColumns = ["reportId"],
                onDelete = ForeignKey.CASCADE,
            )
        ]
)
internal data class BluetoothBeaconEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macAddress: String,
    val age: Long,
    val name: String?,
    val beaconType: Int?,
    val id1: String?,
    val id2: String?,
    val id3: String?,
    val signalStrength: Int?,
    @ColumnInfo(index = true) val reportId: Long?,
) {
    companion object {
        fun fromBluetoothBeacon(
            reportId: Long,
            reportTimestamp: Instant,
            emitterObservation: EmitterObservation<BluetoothBeacon, MacAddress>,
        ): BluetoothBeaconEntity {
            val beacon = emitterObservation.emitter

            val age =
                maxOf(
                    0,
                    Instant.now()
                        .minusMillis(SystemClock.elapsedRealtime() - emitterObservation.timestamp)
                        .until(reportTimestamp, ChronoUnit.MILLIS),
                )

            return BluetoothBeaconEntity(
                id = 0,
                macAddress = beacon.macAddress.value,
                age = age,
                name = null,
                signalStrength = beacon.signalStrength,
                beaconType = beacon.beaconType,
                id1 = beacon.id1,
                id2 = beacon.id2,
                id3 = beacon.id3,
                reportId = reportId,
            )
        }
    }
}

internal fun BluetoothBeaconEntity.toBluetoothBeacon(): BluetoothBeacon {
    return BluetoothBeacon(
        macAddress = MacAddress(macAddress),
        beaconType = beaconType,
        id1 = id1,
        id2 = id2,
        id3 = id3,
        signalStrength = signalStrength!!,
    )
}
