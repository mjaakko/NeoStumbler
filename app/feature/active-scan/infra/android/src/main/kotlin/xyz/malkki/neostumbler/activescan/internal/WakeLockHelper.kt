package xyz.malkki.neostumbler.activescan.internal

import android.annotation.SuppressLint
import android.os.PowerManager
import java.util.concurrent.atomic.AtomicReference

private const val WAKE_LOCK_TAG = "xyz.malkki.neostumbler:ActiveScanService"

internal class WakeLockHelper(private val powerManager: PowerManager) {
    private val wakeLockReference = AtomicReference<PowerManager.WakeLock?>(null)

    /** Acquires a wake lock if not already acquired */
    fun acquireWakeLock() {
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

        if (wakeLockReference.compareAndSet(null, wakeLock)) {
            @SuppressLint("WakelockTimeout") wakeLock.acquire()
        }
    }

    /** Releases the acquired wake lock */
    fun releaseWakeLock() {
        wakeLockReference.getAndSet(null)?.release()
    }
}
