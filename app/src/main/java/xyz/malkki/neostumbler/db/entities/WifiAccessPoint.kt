package xyz.malkki.neostumbler.db.entities

import android.net.wifi.ScanResult
import android.os.Build
import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.extensions.ssidString
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity(foreignKeys = [ForeignKey(entity = Report::class, parentColumns = ["id"], childColumns = ["reportId"], onDelete = ForeignKey.CASCADE)])
data class WifiAccessPoint(
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
        fun createFromScanResult(reportId: Long, currentTime: Instant, scanResult: ScanResult): WifiAccessPoint {
            val radioType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                when (scanResult.wifiStandard) {
                    //ScanResult.WIFI_STANDARD_11AX -> "802.11ax"
                    ScanResult.WIFI_STANDARD_11AC -> "802.11ac"
                    ScanResult.WIFI_STANDARD_11N -> "802.11n"
                    ScanResult.WIFI_STANDARD_LEGACY -> "802.11g" //Could be also 802.11a or 802.11b but g is most likely
                    else -> null
                }
            } else {
                null
            }

            val frequency = when (scanResult.channelWidth) {
                ScanResult.CHANNEL_WIDTH_20MHZ -> scanResult.frequency
                else -> scanResult.centerFreq0
            }

            val channelNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ScanResult.convertFrequencyMhzToChannelIfSupported(frequency).takeIf { it != ScanResult.UNSPECIFIED }
            } else {
                null
            }

            //Current time is truncated to seconds -> age can be negative by some milliseconds
            val age = maxOf(0, Instant.now().minusMillis(SystemClock.elapsedRealtime() - (scanResult.timestamp / 1000)).until(currentTime, ChronoUnit.MILLIS))

            return WifiAccessPoint(
                null,
                scanResult.BSSID,
                radioType,
                age,
                channelNumber,
                frequency,
                scanResult.level,
                null,
                scanResult.ssidString,
                reportId
            )
        }
    }
}
