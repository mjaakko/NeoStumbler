package xyz.malkki.neostumbler.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
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

@IntDef(flag = true, value = [
    Toast.LENGTH_SHORT,
    Toast.LENGTH_LONG
])
@Retention(AnnotationRetention.SOURCE)
annotation class ToastLength


/**
 * Shows a toast message with the specified text
 *
 * @param text Text to show
 * @param length Duration of the toast
 */
fun Context.showToast(text: String, @ToastLength length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}