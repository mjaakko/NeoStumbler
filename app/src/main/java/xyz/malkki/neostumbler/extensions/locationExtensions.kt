package xyz.malkki.neostumbler.extensions

import android.location.Location
import android.os.Build

val Location.elapsedRealtimeMillisCompat: Long
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            elapsedRealtimeMillis
        } else {
            elapsedRealtimeNanos / 1000000
        }
