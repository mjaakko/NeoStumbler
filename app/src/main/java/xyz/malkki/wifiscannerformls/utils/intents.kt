package xyz.malkki.wifiscannerformls.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

fun showMapWithMarkerIntent(latitude: Double, longitude: Double): Intent {
    val latString = "%.8f".format(Locale.ROOT, latitude)
    val lonString = "%.8f".format(Locale.ROOT, longitude)

    return Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("geo:${latString},${lonString}?q=${latString},${lonString}")
    }
}

fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
    }
}