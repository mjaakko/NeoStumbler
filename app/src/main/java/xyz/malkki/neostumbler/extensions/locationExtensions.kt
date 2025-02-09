package xyz.malkki.neostumbler.extensions

import android.location.Location
import android.os.Build

private const val MS_IN_NS = 1_000_000

val Location.elapsedRealtimeMillisCompat: Long
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            elapsedRealtimeMillis
        } else {
            elapsedRealtimeNanos / MS_IN_NS
        }
