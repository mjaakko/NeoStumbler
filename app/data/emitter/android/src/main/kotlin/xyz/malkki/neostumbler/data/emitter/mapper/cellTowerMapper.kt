package xyz.malkki.neostumbler.data.emitter.mapper

import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation

internal fun CellInfo.toCellTower(): EmitterObservation<CellTower, String>? {
    val cellTower =
        when (this) {
            is CellInfoNr -> fromCellInfoNr(this)
            is CellInfoLte -> fromCellInfoLte(this)
            is CellInfoGsm -> fromCellInfoGsm(this)
            is CellInfoWcdma -> fromCellInfoWcdma(this)
            else -> null
        }

    return cellTower?.let { EmitterObservation(emitter = it, timestamp = timestampMillisCompat) }
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
        radioType = CellTower.RadioType.NR,
        mobileCountryCode = cellIdentity.mccString,
        mobileNetworkCode = cellIdentity.mncString,
        cellId = cellIdentity.nci.takeIf { it != CellInfo.UNAVAILABLE_LONG && it != 0L },
        locationAreaCode = cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
        asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
        primaryScramblingCode = cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
        serving = cellInfoNr.serving(),
        timingAdvance = null,
        arfcn = cellIdentity.nrarfcn.takeIf { it != CellInfo.UNAVAILABLE },
        signalStrength = cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
    )
}

private fun fromCellInfoLte(cellInfoLte: CellInfoLte): CellTower {
    val cellSignalStrength = cellInfoLte.cellSignalStrength
    val cellIdentity = cellInfoLte.cellIdentity

    return CellTower(
        radioType = CellTower.RadioType.LTE,
        mobileCountryCode = cellIdentity.mccString,
        mobileNetworkCode = cellIdentity.mncString,
        cellId = cellIdentity.ci.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
        locationAreaCode = cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
        asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
        primaryScramblingCode = cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
        serving = cellInfoLte.serving(),
        timingAdvance = cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
        arfcn = cellIdentity.earfcn.takeIf { it != CellInfo.UNAVAILABLE },
        signalStrength = cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE },
    )
}

private fun fromCellInfoGsm(cellInfoGsm: CellInfoGsm): CellTower {
    val cellSignalStrength = cellInfoGsm.cellSignalStrength
    val cellIdentity = cellInfoGsm.cellIdentity

    return CellTower(
        radioType = CellTower.RadioType.GSM,
        mobileCountryCode = cellIdentity.mccString,
        mobileNetworkCode = cellIdentity.mncString,
        cellId = cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
        locationAreaCode = cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
        asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
        primaryScramblingCode = null,
        serving = cellInfoGsm.serving(),
        timingAdvance = cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
        arfcn = cellIdentity.arfcn.takeIf { it != CellInfo.UNAVAILABLE },
        signalStrength =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cellSignalStrength.rssi.takeIf { it != CellInfo.UNAVAILABLE }
            } else {
                null
            },
    )
}

private fun fromCellInfoWcdma(cellInfoWcdma: CellInfoWcdma): CellTower {
    val cellSignalStrength = cellInfoWcdma.cellSignalStrength
    val cellIdentity = cellInfoWcdma.cellIdentity

    return CellTower(
        radioType = CellTower.RadioType.WCDMA,
        mobileCountryCode = cellIdentity.mccString,
        mobileNetworkCode = cellIdentity.mncString,
        cellId = cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
        locationAreaCode = cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
        asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
        primaryScramblingCode = cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE },
        serving = cellInfoWcdma.serving(),
        timingAdvance = null,
        arfcn = cellIdentity.uarfcn.takeIf { it != CellInfo.UNAVAILABLE },
        signalStrength = cellSignalStrength.dbm.takeIf { it != CellInfo.UNAVAILABLE },
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
