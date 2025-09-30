package xyz.malkki.neostumbler.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

// This build flavor does not have to be compliant with Google Play policies
@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimizations(packageName: String): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:$packageName".toUri()
    }
}
