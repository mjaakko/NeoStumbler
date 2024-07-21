package xyz.malkki.neostumbler.domain

import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import xyz.malkki.neostumbler.extensions.timestampMillisCompat

data class CellTower(
    val radioType: RadioType,
    val mobileCountryCode: String?,
    val mobileNetworkCode: String?,
    val cellId: Long?,
    val locationAreaCode: Int?,
    val asu: Int?,
    val primaryScramblingCode: Int?,
    val serving: Int?,
    val signalStrength: Int?,
    val timingAdvance: Int?,
    /**
     * Timestamp when the cell tower was observed in milliseconds since boot
     */
    val timestamp: Long
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

        fun fromCellInfo(cellInfo: CellInfo): CellTower? {
            return when (cellInfo) {
                is CellInfoNr -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                    val cellIdentity = cellInfo.cellIdentity as CellIdentityNr

                    CellTower(
                        RadioType.NR,
                        cellIdentity.mccString,
                        cellIdentity.mncString,
                        cellIdentity.nci.takeIf { it != CellInfo.UNAVAILABLE_LONG && it != 0L },
                        cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        cellInfo.timestampMillisCompat
                    )
                }

                is CellInfoLte -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        RadioType.LTE,
                        cellIdentity.mccString,
                        cellIdentity.mncString,
                        cellIdentity.ci.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                        cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE },
                        cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.timestampMillisCompat
                    )
                }

                is CellInfoGsm -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        RadioType.GSM,
                        cellIdentity.mccString,
                        cellIdentity.mncString,
                        cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                        cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        cellInfo.serving(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE }
                        } else {
                            null
                        },
                        cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.timestampMillisCompat
                    )
                }

                is CellInfoWcdma -> {
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    val cellIdentity = cellInfo.cellIdentity

                    CellTower(
                        RadioType.WCDMA,
                        cellIdentity.mccString,
                        cellIdentity.mncString,
                        cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                        cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                        cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                        cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE },
                        cellInfo.serving(),
                        cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                        null,
                        cellInfo.timestampMillisCompat
                    )
                }

                else -> null
            }
        }
    }

    enum class RadioType {
        GSM, WCDMA, LTE, NR
    }
}