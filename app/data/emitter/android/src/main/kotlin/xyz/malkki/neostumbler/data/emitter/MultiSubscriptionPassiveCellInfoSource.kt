package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation

class MultiSubscriptionPassiveCellInfoSource(
    private val subscriptionManager: SubscriptionManager,
    private val telephonyManager: TelephonyManager,
) : PassiveCellTowerSource {
    constructor(
        context: Context
    ) : this(
        context.getSystemService<SubscriptionManager>()!!,
        context.getSystemService<TelephonyManager>()!!,
    )

    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            ]
    )
    override fun getCellTowers(): List<EmitterObservation<CellTower, String>> {
        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

        return activeSubscriptions
            .flatMap {
                TelephonyManagerPassiveCellInfoSource(
                        telephonyManager.createForSubscriptionId(it.subscriptionId)
                    )
                    .getCellTowers()
            }
            .distinctBy {
                // Filter duplicates in case different subscriptions use the same cell towers
                it.emitter.uniqueKey
            }
    }
}
