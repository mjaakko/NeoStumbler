package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.core.CellTower

class MultiSubscriptionPassiveCellInfoSource(context: Context) : PassiveCellTowerSource {
    private val subscriptionManager = context.getSystemService<SubscriptionManager>()!!
    private val telephonyManager = context.getSystemService<TelephonyManager>()!!

    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            ]
    )
    override fun getCellTowers(): List<CellTower> {
        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

        return activeSubscriptions.flatMap {
            TelephonyManagerPassiveCellInfoSource(
                    telephonyManager.createForSubscriptionId(it.subscriptionId)
                )
                .getCellTowers()
        }
    }
}
