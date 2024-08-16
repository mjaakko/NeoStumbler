package xyz.malkki.neostumbler.db.entities

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity(
    foreignKeys = [ForeignKey(entity = Report::class, parentColumns = ["id"], childColumns = ["reportId"], onDelete = ForeignKey.CASCADE)]
)
data class WifiAccessPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val macAddress: String,
    val radioType: String?,
    val age: Long,
    val channel: Int?,
    val frequency: Int?,
    val signalStrength: Int?,
    val signalToNoiseRatio: Int?,
    val ssid: String?,
    @ColumnInfo(index = true) val reportId: Long?
) {
    companion object {
        fun fromWifiAccessPoint(wifiAccessPoint: WifiAccessPoint, reportTimestamp: Instant, reportId: Long): WifiAccessPointEntity {
            //Report time is truncated to seconds -> age can be negative by some milliseconds
            val age = maxOf(0, Instant.now().minusMillis(SystemClock.elapsedRealtime() - (wifiAccessPoint.timestamp)).until(reportTimestamp, ChronoUnit.MILLIS))

            return WifiAccessPointEntity(
                id = null,
                macAddress = wifiAccessPoint.macAddress,
                radioType = when (wifiAccessPoint.radioType) {
                    WifiAccessPoint.RadioType.BE -> "802.11be"
                    WifiAccessPoint.RadioType.AX -> "802.11ax"
                    WifiAccessPoint.RadioType.AC -> "802.11ac"
                    WifiAccessPoint.RadioType.N -> "802.11n"
                    WifiAccessPoint.RadioType.G -> "802.11g"
                    else -> null
                },
                age = age,
                channel = wifiAccessPoint.channel,
                frequency = wifiAccessPoint.frequency,
                signalStrength = wifiAccessPoint.signalStrength,
                signalToNoiseRatio = null,
                ssid = wifiAccessPoint.ssid,
                reportId = reportId
            )
        }
    }
}
