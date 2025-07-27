package xyz.malkki.neostumbler.db.entities

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
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
internal data class WifiAccessPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val macAddress: String,
    val radioType: String?,
    val age: Long,
    val channel: Int?,
    val frequency: Int?,
    val signalStrength: Int?,
    val ssid: String?,
    @ColumnInfo(index = true) val reportId: Long?,
) {
    companion object {
        fun fromWifiAccessPoint(
            emitterObservation: EmitterObservation<WifiAccessPoint, MacAddress>,
            reportTimestamp: Instant,
            reportId: Long,
        ): WifiAccessPointEntity {
            val wifiAccessPoint = emitterObservation.emitter

            // Report time is truncated to seconds -> age can be negative by some milliseconds
            val age =
                maxOf(
                    0,
                    Instant.now()
                        .minusMillis(SystemClock.elapsedRealtime() - (emitterObservation.timestamp))
                        .until(reportTimestamp, ChronoUnit.MILLIS),
                )

            return WifiAccessPointEntity(
                id = null,
                macAddress = wifiAccessPoint.macAddress.value,
                radioType = wifiAccessPoint.radioType?.to802String(),
                age = age,
                channel = wifiAccessPoint.channel,
                frequency = wifiAccessPoint.frequency,
                signalStrength = wifiAccessPoint.signalStrength,
                ssid = wifiAccessPoint.ssid,
                reportId = reportId,
            )
        }
    }
}

internal fun WifiAccessPointEntity.toWifiAccessPoint(): WifiAccessPoint {
    return WifiAccessPoint(
        macAddress = MacAddress(macAddress),
        radioType = radioType?.let { WifiAccessPoint.RadioType.from802String(it) },
        channel = channel,
        frequency = frequency,
        ssid = ssid,
        signalStrength = signalStrength,
    )
}
