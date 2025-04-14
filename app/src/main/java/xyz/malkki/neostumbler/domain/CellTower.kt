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
    val arfcn: Int?,
    /** Timestamp when the cell tower was observed in milliseconds since boot */
    override val timestamp: Long,
) : ObservedDevice<String> {
    override val uniqueKey: String
        get() =
            listOf(
                    mobileCountryCode,
                    mobileNetworkCode,
                    locationAreaCode,
                    cellId,
                    primaryScramblingCode,
                )
                .joinToString("/")

    companion object {
        private fun CellInfo.serving(): Int? {
            if (cellConnectionStatus == CellInfo.CONNECTION_UNKNOWN) {
                return null
            }

            return if (
                cellConnectionStatus == CellInfo.CONNECTION_PRIMARY_SERVING ||
                    cellConnectionStatus == CellInfo.CONNECTION_SECONDARY_SERVING
            ) {
                1
            } else {
                0
            }
        }

        private fun fromCellInfoNr(cellInfoNr: CellInfoNr): CellTower {
            val cellSignalStrength = cellInfoNr.cellSignalStrength as CellSignalStrengthNr
            val cellIdentity = cellInfoNr.cellIdentity as CellIdentityNr

            return CellTower(
                RadioType.NR,
                cellIdentity.mccString,
                cellIdentity.mncString,
                cellIdentity.nci.takeIf { it != CellInfo.UNAVAILABLE_LONG && it != 0L },
                cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoNr.serving(),
                cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                null,
                cellIdentity.nrarfcn.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoNr.timestampMillisCompat,
            )
        }

        private fun fromCellInfoLte(cellInfoLte: CellInfoLte): CellTower {
            val cellSignalStrength = cellInfoLte.cellSignalStrength
            val cellIdentity = cellInfoLte.cellIdentity

            return CellTower(
                RadioType.LTE,
                cellIdentity.mccString,
                cellIdentity.mncString,
                cellIdentity.ci.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoLte.serving(),
                cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE },
                cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                cellIdentity.earfcn.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoLte.timestampMillisCompat,
            )
        }

        private fun fromCellInfoGsm(cellInfoGsm: CellInfoGsm): CellTower {
            val cellSignalStrength = cellInfoGsm.cellSignalStrength
            val cellIdentity = cellInfoGsm.cellIdentity

            return CellTower(
                RadioType.GSM,
                cellIdentity.mccString,
                cellIdentity.mncString,
                cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                null,
                cellInfoGsm.serving(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE }
                } else {
                    null
                },
                cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
                cellIdentity.arfcn.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoGsm.timestampMillisCompat,
            )
        }

        private fun fromCellInfoWcdma(cellInfoWcdma: CellInfoWcdma): CellTower {
            val cellSignalStrength = cellInfoWcdma.cellSignalStrength
            val cellIdentity = cellInfoWcdma.cellIdentity

            return CellTower(
                RadioType.WCDMA,
                cellIdentity.mccString,
                cellIdentity.mncString,
                cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
                cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
                cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
                cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoWcdma.serving(),
                cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
                null,
                cellIdentity.uarfcn.takeIf { it != CellInfo.UNAVAILABLE },
                cellInfoWcdma.timestampMillisCompat,
            )
        }

        fun fromCellInfo(cellInfo: CellInfo): CellTower? {
            return when (cellInfo) {
                is CellInfoNr -> fromCellInfoNr(cellInfo)
                is CellInfoLte -> fromCellInfoLte(cellInfo)
                is CellInfoGsm -> fromCellInfoGsm(cellInfo)
                is CellInfoWcdma -> fromCellInfoWcdma(cellInfo)
                else -> null
            }
        }

        /**
         * Fills missing data from other cell towers in the list
         *
         * @param operatorNumeric Combination of MCC / MNC (same as
         *   [android.telephony.ServiceState.getOperatorNumeric])
         */
        fun List<CellTower>.fillMissingData(operatorNumeric: String?): List<CellTower> {
            if (operatorNumeric == null && size == 1) {
                return this
            } else {
                val mobileCountryCodes = mapNotNull { it.mobileCountryCode }.toSet()
                val mobileNetworkCodes = mapNotNull { it.mobileNetworkCode }.toSet()

                if (mobileCountryCodes.size != 1) {
                    // MCC not unique, we can't be sure about which MNC to use
                    return this
                }

                val mcc = mobileCountryCodes.first()

                val mnc =
                    if (mobileNetworkCodes.size == 1) {
                        mobileNetworkCodes.first()
                    } else if (operatorNumeric?.startsWith(mcc) == true) {
                        operatorNumeric.replaceFirst(mcc, "")
                    } else {
                        null
                    }

                if (mnc == null) {
                    return this
                }

                return map { cellTower ->
                    cellTower.copy(mobileCountryCode = mcc, mobileNetworkCode = mnc)
                }
            }
        }
    }

    /**
     * Checks if the cell info has enough useful data. Used for filtering neighbouring cells which
     * don't specify their cell ID etc.
     */
    fun hasEnoughData(): Boolean {
        if (mobileCountryCode == null || mobileNetworkCode == null) {
            return false
        }

        return when (radioType) {
            RadioType.GSM -> cellId != null || locationAreaCode != null
            RadioType.WCDMA,
            RadioType.LTE,
            RadioType.NR ->
                cellId != null || locationAreaCode != null || primaryScramblingCode != null
        }
    }

    enum class RadioType {
        GSM,
        WCDMA,
        LTE,
        NR,
    }
}
