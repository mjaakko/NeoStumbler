package xyz.malkki.neostumbler.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

// This build flavor does not have to be compliant with Google Play policies
@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:$packageName".toUri()
    }
}

fun openUrl(url: String): Intent {
    return CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()
        .intent
        .apply { data = url.toUri() }
}
