package xyz.malkki.neostumbler.utils

import android.content.Intent
import android.provider.Settings

fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
