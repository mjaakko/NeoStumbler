package xyz.malkki.neostumbler.scanner.source

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.sample
import timber.log.Timber
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.extensions.combineAny
import xyz.malkki.neostumbler.extensions.getActiveSubscriptionIds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MultiSubscriptionCellInfoSource(private val context: Context) : CellInfoSource {
    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun getCellInfoFlow(interval: Duration): Flow<List<CellTower>> {
        val subscriptionManager = context.getSystemService<SubscriptionManager>()!!
        val telephonyManager = context.getSystemService<TelephonyManager>()!!

        //Use slightly smaller scan interval for each subscription so that data does not get delayed when we combine data from different subscriptions
        val scanIntervalPerSubscription = interval.minus(3.seconds).coerceAtLeast(1.seconds)

        return subscriptionManager.getActiveSubscriptionIds()
            .flatMapLatest { activeSubscriptionIds ->
                Timber.i("Active mobile subscriptions changed, currently active subscription IDs: $activeSubscriptionIds")

                activeSubscriptionIds
                    .map { subscriptionId ->
                        val subscriptionTelephonyManager =
                            telephonyManager.createForSubscriptionId(subscriptionId)

                        TelephonyManagerCellInfoSource(subscriptionTelephonyManager).getCellInfoFlow(scanIntervalPerSubscription)
                    }
                    .combineAny { cellInfoLists ->
                        cellInfoLists
                            .filterNotNull()
                            .flatten()
                            //Remove duplicate cells in case the subscriptions use the same operator
                            .distinctBy { it.getKey() }
                    }
            }
            .sample(interval)
    }
}

/**
 * Returns a key used for filtering duplicate cells
 */
private fun CellTower.getKey(): String {
    return "${radioType}/${mobileCountryCode}/${mobileNetworkCode}/${locationAreaCode}/${cellId}/${primaryScramblingCode}"
}