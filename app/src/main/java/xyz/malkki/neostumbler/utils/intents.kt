package xyz.malkki.neostumbler.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
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
