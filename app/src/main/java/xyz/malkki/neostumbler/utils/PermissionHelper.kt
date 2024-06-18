package xyz.malkki.neostumbler.utils

import android.Manifest
import android.content.Context
import android.os.Build
import xyz.malkki.neostumbler.extensions.checkMissingPermissions

object PermissionHelper {
    fun hasScanPermissions(context: Context): Boolean = context
        .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        .isEmpty()

    fun hasAutoScanPermissions(context: Context): Boolean = context
        .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION)
        .isEmpty()
}