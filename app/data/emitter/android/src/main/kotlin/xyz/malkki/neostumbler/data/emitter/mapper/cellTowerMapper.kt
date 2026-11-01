package xyz.malkki.neostumbler.data.emitter.mapper

import android.os.Build
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.values.SignalStrength

internal fun CellInfo.toCellTowers(): List<EmitterObservation<CellTower, String>> {
    val cellTowers =
        when (this) {
            is CellInfoNr -> toCellTowers()
            is CellInfoLte -> toCellTowers()
            is CellInfoGsm -> toCellTowers()
            is CellInfoWcdma -> toCellTowers()
            else -> emptyList()
        }

    return cellTowers.map { EmitterObservation(emitter = it, timestamp = timestampMillisCompat) }
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

private fun CellInfoNr.toCellTowers(): List<CellTower> {
    val cellSignalStrength = cellSignalStrength as CellSignalStrengthNr
    val cellIdentity = cellIdentity as CellIdentityNr

    val baseTower =
        CellTower(
            radioType = CellTower.RadioType.NR,
            mobileCountryCode = cellIdentity.mccString,
            mobileNetworkCode = cellIdentity.mncString,
            cellId = cellIdentity.nci.takeIf { it != CellInfo.UNAVAILABLE_LONG && it != 0L },
            locationAreaCode = cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
            asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
            primaryScramblingCode = cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
            serving = serving(),
            timingAdvance = null,
            arfcn = cellIdentity.nrarfcn.takeIf { it != CellInfo.UNAVAILABLE },
            signalStrength =
                cellSignalStrength.dbm
                    .takeIf { it != CellInfo.UNAVAILABLE }
                    ?.let { SignalStrength(it) },
        )

    return baseTower.createCopiesWithAdditionalPlmns(cellIdentity)
}

private fun CellInfoLte.toCellTowers(): List<CellTower> {
    val baseTower =
        CellTower(
            radioType = CellTower.RadioType.LTE,
            mobileCountryCode = cellIdentity.mccString,
            mobileNetworkCode = cellIdentity.mncString,
            cellId = cellIdentity.ci.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
            locationAreaCode = cellIdentity.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
            asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
            primaryScramblingCode = cellIdentity.pci.takeIf { it != CellInfo.UNAVAILABLE },
            serving = serving(),
            timingAdvance = cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
            arfcn = cellIdentity.earfcn.takeIf { it != CellInfo.UNAVAILABLE },
            signalStrength =
                cellSignalStrength.rssi
                    .takeIf { it != CellInfo.UNAVAILABLE }
                    ?.let { SignalStrength(it) },
        )

    return baseTower.createCopiesWithAdditionalPlmns(cellIdentity)
}

private fun CellInfoGsm.toCellTowers(): List<CellTower> {
    val baseTower =
        CellTower(
            radioType = CellTower.RadioType.GSM,
            mobileCountryCode = cellIdentity.mccString,
            mobileNetworkCode = cellIdentity.mncString,
            cellId = cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
            locationAreaCode = cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
            asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
            primaryScramblingCode = null,
            serving = serving(),
            timingAdvance = cellSignalStrength.timingAdvance.takeIf { it != CellInfo.UNAVAILABLE },
            arfcn = cellIdentity.arfcn.takeIf { it != CellInfo.UNAVAILABLE },
            signalStrength =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cellSignalStrength.rssi
                        .takeIf { it != CellInfo.UNAVAILABLE }
                        ?.let { SignalStrength(it) }
                } else {
                    null
                },
        )

    return baseTower.createCopiesWithAdditionalPlmns(cellIdentity)
}

private fun CellInfoWcdma.toCellTowers(): List<CellTower> {
    val baseTower =
        CellTower(
            radioType = CellTower.RadioType.WCDMA,
            mobileCountryCode = cellIdentity.mccString,
            mobileNetworkCode = cellIdentity.mncString,
            cellId = cellIdentity.cid.takeIf { it != CellInfo.UNAVAILABLE && it != 0 }?.toLong(),
            locationAreaCode = cellIdentity.lac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
            asu = cellSignalStrength.asuLevel.takeIf { it != CellInfo.UNAVAILABLE },
            primaryScramblingCode = cellIdentity.psc.takeIf { it != CellInfo.UNAVAILABLE },
            serving = serving(),
            timingAdvance = null,
            arfcn = cellIdentity.uarfcn.takeIf { it != CellInfo.UNAVAILABLE },
            signalStrength =
                cellSignalStrength.dbm
                    .takeIf { it != CellInfo.UNAVAILABLE }
                    ?.let { SignalStrength(it) },
        )

    return baseTower.createCopiesWithAdditionalPlmns(cellIdentity)
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

private fun CellTower.createCopiesWithAdditionalPlmns(cellIdentity: CellIdentity): List<CellTower> {
    val additionalPlmns = cellIdentity.getAdditionalPlmnsCompat()
    if (additionalPlmns.isEmpty()) {
        return listOf(this)
    }

    val additionalTowers = additionalPlmns.mapNotNull { plmn ->
        val (mcc, mnc) = plmn.parsePlmn()

        if (mcc == null || mnc == null) {
            null
        } else {
            copy(mobileCountryCode = mcc, mobileNetworkCode = mnc)
        }
    }

    return listOf(this) + additionalTowers
}

/**
 * Parses a PLMN string (e.g. "24403") into MCC and MNC.
 *
 * @return a pair of (MCC, MNC) or null if the PLMN couldn't be parsed
 */
private fun String.parsePlmn(): Pair<String?, String?> {
    if (length !in 5..6) {
        return null to null
    }

    val mcc = take(3)
    val mnc = drop(3)

    if (!mcc.all { it.isDigit() } || !mnc.all { it.isDigit() }) {
        return null to null
    }

    return mcc to mnc
}

private fun CellIdentity.getAdditionalPlmnsCompat(): Set<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (this) {
            is CellIdentityGsm -> this.additionalPlmns
            is CellIdentityWcdma -> this.additionalPlmns
            is CellIdentityLte -> this.additionalPlmns
            is CellIdentityNr -> this.additionalPlmns
            else -> emptySet()
        }
    } else {
        emptySet()
    }
}
