package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import timber.log.Timber
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.getActiveSubscriptionIds

class MultiSubscriptionActiveCellInfoSource(private val context: Context) : ActiveCellInfoSource {
    @RequiresPermission(
        allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION]
    )
    override fun getCellInfoFlow(
        interval: Flow<Duration>
    ): Flow<List<EmitterObservation<CellTower, String>>> {
        val subscriptionManager = context.getSystemService<SubscriptionManager>()!!
        val telephonyManager = context.getSystemService<TelephonyManager>()!!

        return subscriptionManager.getActiveSubscriptionIds().flatMapLatest { activeSubscriptionIds
            ->
            Timber.i(
                "Active mobile subscriptions changed, currently active subscription IDs: $activeSubscriptionIds"
            )

            activeSubscriptionIds
                .map { subscriptionId ->
                    val subscriptionTelephonyManager =
                        telephonyManager.createForSubscriptionId(subscriptionId)

                    TelephonyManagerActiveCellInfoSource(subscriptionTelephonyManager)
                        .getCellInfoFlow(interval)
                }
                .merge()
        }
    }
}
