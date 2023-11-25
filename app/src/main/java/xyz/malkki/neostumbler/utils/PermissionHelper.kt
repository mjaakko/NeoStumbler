package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.os.Build
import xyz.malkki.neostumbler.extensions.checkMissingPermissions

//TODO: move texts to the strings.xml file
object PermissionHelper {
    val PERMISSION_RATIONALES = mutableMapOf<String, String>()
        .apply {
            put(Manifest.permission.ACCESS_FINE_LOCATION, "Scanning Wi-Fi networks needs access to exact location")
            put(Manifest.permission.ACTIVITY_RECOGNITION, "Automatically starting scanning needs access to current activity")
            put(Manifest.permission.ACCESS_BACKGROUND_LOCATION, "Automatically starting scanning needs access to background location")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                put(Manifest.permission.POST_NOTIFICATIONS, "Showing status notification needs notification permission")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                put(Manifest.permission.BLUETOOTH_SCAN, "Scanning Bluetooth devices needs access to Bluetooth permission")
            } else {
                put(Manifest.permission.BLUETOOTH, "Scanning Bluetooth devices needs access to Bluetooth permission")
                put(Manifest.permission.BLUETOOTH_ADMIN, "Scanning Bluetooth devices needs access to Bluetooth permission")
            }
        }
        .toMap()

    fun hasScanPermissions(context: Context): Boolean = context
        .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        .isEmpty()

    fun hasAutoScanPermissions(context: Context): Boolean = context
        .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION)
        .isEmpty()
}