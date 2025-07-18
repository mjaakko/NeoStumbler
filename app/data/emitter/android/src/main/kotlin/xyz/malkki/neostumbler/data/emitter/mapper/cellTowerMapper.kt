package xyz.malkki.neostumbler.data.emitter.mapper

import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import xyz.malkki.neostumbler.core.CellTower
import xyz.malkki.neostumbler.core.CellTower.RadioType

internal fun CellInfo.toCellTower(): CellTower? {
    return when (this) {
        is CellInfoNr -> fromCellInfoNr(this)
        is CellInfoLte -> fromCellInfoLte(this)
        is CellInfoGsm -> fromCellInfoGsm(this)
        is CellInfoWcdma -> fromCellInfoWcdma(this)
        else -> null
    }
}

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

private const val MS_IN_NS = 1_000_000

/** CellInfo timestamp in milliseconds since boot */
private val CellInfo.timestampMillisCompat: Long
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            timestampMillis
        } else {
            @Suppress("DEPRECATION")
            timeStamp / MS_IN_NS
        }
