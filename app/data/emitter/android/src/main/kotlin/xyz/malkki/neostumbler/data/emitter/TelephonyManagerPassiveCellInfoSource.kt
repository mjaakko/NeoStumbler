package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.CellTower.Companion.fillMissingData
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.mapper.toCellTower

class TelephonyManagerPassiveCellInfoSource(private val telephonyManager: TelephonyManager) :
    PassiveCellTowerSource {
    constructor(context: Context) : this(context.getSystemService<TelephonyManager>()!!)

    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            ]
    )
    override fun getCellTowers(): List<EmitterObservation<CellTower, String>> {
        val serviceState = telephonyManager.serviceState

        return telephonyManager.allCellInfo
            .mapNotNull { cellInfo -> cellInfo.toCellTower() }
            .fillMissingData(serviceState?.operatorNumeric)
            .filterByOperator(serviceState?.operatorNumeric)
    }

    private fun List<EmitterObservation<CellTower, String>>.filterByOperator(
        operatorNumeric: String?
    ): List<EmitterObservation<CellTower, String>> {
        if (
            map { it.emitter.mobileCountryCode to it.emitter.mobileNetworkCode }
                .distinct()
                .count() == 1
        ) {
            return this
        }

        /**
         * [TelephonyManager.getAllCellInfo] can return cells for other subscriptions on the device
         * -> filter them based on the operator code in the [android.telephony.ServiceState]
         */
        return filter {
            (it.emitter.mobileCountryCode + it.emitter.mobileNetworkCode) == operatorNumeric
        }
    }
}
