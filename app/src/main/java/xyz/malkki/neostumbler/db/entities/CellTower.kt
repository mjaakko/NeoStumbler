package xyz.malkki.neostumbler.db.entities

import android.os.Build
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.extensions.timestampMillisCompat
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity(foreignKeys = [ForeignKey(entity = Report::class, parentColumns = ["id"], childColumns = ["reportId"], onDelete = ForeignKey.CASCADE)])
data class CellTower(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val radioType: String,
    val mobileCountryCode: Int?,
    val mobileNetworkCode: Int?,
    val cellId: Int?,
    val locationAreaCode: Int?,
    val asu: Int?,
    val primaryScramblingCode: Int?,
    val serving: Int?,
    val signalStrength: Int?,
    val timingAdvance: Int?,
    val age: Long,
    @ColumnInfo(index = true) val reportId: Long?
) {
    companion object {
        private fun CellInfo.serving(): Int? {
            if (cellConnectionStatus == CellInfo.CONNECTION_UNKNOWN) {
                return null
            }

            return if (cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING || cellConnectionStatus == CellInfo.CONNECTION_SECONDARY_SERVING) {
                1
            } else {
                0
            }
        }

        fun fromCellInfo(reportId: Long, currentTime: Instant, cellInfo: CellInfo): CellTower? {
            //Current time is truncated to seconds -> age can be negative by some milliseconds
            val age = maxOf(0, Instant.now().minusMillis(SystemClock.elapsedRealtime() - cellInfo.timestampMillisCompat).until(currentTime, ChronoUnit.MILLIS))

            return when (cellInfo) {
                is CellInfoNr -> {
                    null

                    //MLS currently does not support 5G towers

                    /*val cellSignalStrength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentity = cellInfo.cellIdentity as CellIdentityNr

                    CellTower(
                        null,
                        "nr",
                        cellIdentity.mccString?.toIntOrNull(),
                        cellIdentity.mncString?.toIntOrNull(),
                        cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        age,
                        reportId
                    )*/
                }
                is CellInfoLte -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        null,
                        "lte",
                        cellIdentity.mccString?.toIntOrNull(),
                        cellIdentity.mncString?.toIntOrNull(),
                        cellIdentity.ci.takeIf { it != CellInfo.UNAVAILABLE && it != 0  },
                        cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0  },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE },
                        cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                        age,
                        reportId
                    )
                }
                is CellInfoGsm -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        null,
                        "gsm",
                        cellIdentity.mccString?.toIntOrNull(),
                        cellIdentity.mncString?.toIntOrNull(),
                        cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                        cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0  },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        cellInfo.serving(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE } } else { null },
                        cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                        age,
                        reportId
                    )
                }
                is CellInfoWcdma -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        null,
                        "wcdma",
                        cellIdentity.mccString?.toIntOrNull(),
                        cellIdentity.mncString?.toIntOrNull(),
                        cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0  },
                        cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0  },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        age,
                        reportId
                    )
                }
                else -> null
            }
        }
    }
}
