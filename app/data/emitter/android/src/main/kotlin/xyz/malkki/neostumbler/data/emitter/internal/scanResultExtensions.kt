package xyz.malkki.neostumbler.data.emitter.internal

import android.bluetooth.le.ScanResult

private const val MS_IN_NS = 1_000_000

internal val ScanResult.timestampMillis: Long
    get() = timestampNanos / MS_IN_NS
