package xyz.malkki.neostumbler.extensions

import android.os.Build
import android.telephony.CellInfo

/**
 * CellInfo timestamp in milliseconds since boot
 */
val CellInfo.timestampMillisCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        timestampMillis
    } else {
        @Suppress("DEPRECATION")
        timeStamp / 1_000_000
    }