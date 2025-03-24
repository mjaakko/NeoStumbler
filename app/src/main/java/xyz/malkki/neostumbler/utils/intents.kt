package xyz.malkki.neostumbler.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
    }
}
