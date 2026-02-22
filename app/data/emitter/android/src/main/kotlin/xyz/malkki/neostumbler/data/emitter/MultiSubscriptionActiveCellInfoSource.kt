package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import timber.log.Timber
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.getActiveSubscriptionIds

class MultiSubscriptionActiveCellInfoSource(context: Context) : ActiveCellInfoSource {
    private val appContext: Context = context.applicationContext

    override fun getCellInfoFlow(
        interval: Flow<Duration>
    ): Flow<List<EmitterObservation<CellTower, String>>> {
        if (!appContext.hasCellScanPermission()) {
            Timber.w("No cell tower scan permission, not scanning for cell towers")

            return emptyFlow()
        }

        val subscriptionManager = appContext.getSystemService<SubscriptionManager>()!!
        val telephonyManager = appContext.getSystemService<TelephonyManager>()!!

        @SuppressLint("MissingPermission")
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

    private fun Context.hasCellScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}
