package xyz.malkki.neostumbler.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.Locale

/**
 * Checks which permissions the application is missing
 *
 * @return List of missing permissions
 */
fun Context.checkMissingPermissions(vararg permissions: String): List<String> = permissions.filter { checkSelfPermission(it) == PackageManager.PERMISSION_DENIED }

/**
 * Returns activity of the context or null when the context is not an activity
 *
 * @return Activity or null
 */
fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

val Context.localeList: LocaleListCompat
    get() = LocaleListCompat.wrap(resources.configuration.locales)

val Context.defaultLocale: Locale
    get() = localeList[0]!!

/**
 * Checks if Wi-Fi scan throttling is enabled
 *
 * @return True is scan throttling is enabled, false if not, and null if could not be determined
 */
fun Context.isWifiScanThrottled(): Boolean? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getSystemService<WifiManager>()!!.isScanThrottleEnabled
    } else {
        when (Settings.Global.getInt(contentResolver, "wifi_scan_throttle_enabled", -1)) {
            1 -> true
            0 -> false
            else -> null
        }
    }
}

/**
 * Checks if Google APIs are available
 *
 * @return true is Google APIs are available, false if not
 */
fun Context.isGoogleApisAvailable(): Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext) == ConnectionResult.SUCCESS
}