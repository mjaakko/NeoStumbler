package xyz.malkki.neostumbler.activescan.adapter

import android.app.Notification
import android.content.Context
import android.content.Intent
import java.time.Instant
import xyz.malkki.neostumbler.activescan.ScanState
import xyz.malkki.neostumbler.data.location.GpsStatus

enum class NotificationStyle {
    MINIMAL,
    BASIC,
    DETAILED,
}

data class NotificationParams(
    val notificationStyle: NotificationStyle,
    val startedAt: Instant,
    val state: ScanState,
    val autostarted: Boolean,
    val reportsCreated: Int,
    val gpsStatus: GpsStatus?,
    val stopIntent: Intent,
)

fun interface ScanNotificationAdapter {
    fun createNotification(context: Context, params: NotificationParams): Notification
}
