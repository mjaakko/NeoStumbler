package xyz.malkki.neostumbler.data.emitter.ranging

import android.Manifest
import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.emitter.internal.WifiRttException
import xyz.malkki.neostumbler.data.emitter.internal.doRanging

private val RTT_RANGING_TIMEOUT = 1.seconds

@RequiresPermission(
    allOf =
        [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
        ]
)
internal suspend fun WifiRttManager.rangeDistances(
    scanResults: List<ScanResult>,
    rangingMode: ActiveWifiAccessPointSource.RangingMode,
): List<RangingResult> {
    if (rangingMode == ActiveWifiAccessPointSource.RangingMode.NEVER || !isAvailable) {
        return emptyList()
    }

    val rangingCapableAccessPoints =
        scanResults
            .filter {
                it.is80211mcResponder ||
                    rangingMode == ActiveWifiAccessPointSource.RangingMode.ALWAYS
            }
            .getRttRangingCapableAccessPoints()

    if (rangingCapableAccessPoints.isEmpty()) {
        return emptyList()
    }

    return withTimeoutOrNull(RTT_RANGING_TIMEOUT) {
        try {
            val rangingRequest =
                RangingRequest.Builder().addAccessPoints(rangingCapableAccessPoints).build()

            doRanging(rangingRequest)
        } catch (e: WifiRttException) {
            Timber.w(e, "Wi-Fi RTT ranging failed: %d", e.errorCode)

            emptyList()
        }
    } ?: emptyList()
}

internal fun Collection<ScanResult>.getRttRangingCapableAccessPoints(
    maxResults: Int = RangingRequest.getMaxPeers(),
    isAtLeastAndroid12: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
): List<ScanResult> {
    return sortedWith(
            Comparator
                // Prefer access points that support 2-way ranging
                .comparing<ScanResult, Boolean> { it.is80211mcResponder }
                .thenBy {
                    @SuppressLint("NewApi")
                    isAtLeastAndroid12 && it.is80211azNtbResponder
                }
                .reversed()
                /**
                 * Then sort by signal strength to prefer access points where the ranging is most
                 * likely to succeed
                 */
                .thenByDescending { it.level }
        )
        .take(maxResults)
}
