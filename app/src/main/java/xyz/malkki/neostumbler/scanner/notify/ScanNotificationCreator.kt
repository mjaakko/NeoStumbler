package xyz.malkki.neostumbler.scanner.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_DEFERRED
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.PendingIntentCompat
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.activescan.ScanState
import xyz.malkki.neostumbler.activescan.adapter.NotificationParams
import xyz.malkki.neostumbler.activescan.adapter.NotificationStyle
import xyz.malkki.neostumbler.activescan.adapter.ScanNotificationAdapter
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.getTextCompat

private const val MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE = 4321
private const val STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE = 15415

class ScanNotificationCreator : ScanNotificationAdapter {
    override fun createNotification(context: Context, params: NotificationParams): Notification {
        val reportsCreatedText =
            context.applicationContext.getQuantityString(
                R.plurals.reports_created,
                params.reportsCreated,
                params.reportsCreated,
            )
        val satellitesInUseText =
            params.gpsStatus?.let {
                context.applicationContext
                    .getTextCompat(R.string.satellites_in_use)
                    .toString()
                    .format(it.satellitesUsedInFix, it.satellitesTotal)
            }

        return NotificationCompat.Builder(
                context,
                StumblerApplication.STUMBLING_NOTIFICATION_CHANNEL_ID,
            )
            .apply {
                setSmallIcon(R.drawable.radar_24px)

                setContentTitle(
                    context.applicationContext.getTextCompat(
                        if (params.state is ScanState.Paused) {
                            R.string.scanning_status_paused
                        } else {
                            R.string.scanning_status_active
                        }
                    )
                )

                if (
                    params.notificationStyle == NotificationStyle.BASIC ||
                        (params.notificationStyle >= NotificationStyle.BASIC &&
                            satellitesInUseText == null)
                ) {
                    setContentText(reportsCreatedText)
                } else if (params.notificationStyle >= NotificationStyle.BASIC) {
                    setContentText("$reportsCreatedText | $satellitesInUseText")

                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                """
                        $reportsCreatedText
                       
                        $satellitesInUseText
                    """
                                    .trimIndent()
                            )
                    )
                }

                setPriority(NotificationCompat.PRIORITY_LOW)

                setOngoing(true)
                setAllowSystemGeneratedContextualActions(false)
                setOnlyAlertOnce(true)
                setLocalOnly(true)

                setCategory(Notification.CATEGORY_SERVICE)

                setForegroundServiceBehavior(
                    if (params.autostarted) {
                        FOREGROUND_SERVICE_DEFERRED
                    } else {
                        FOREGROUND_SERVICE_IMMEDIATE
                    }
                )

                setUsesChronometer(true)
                setShowWhen(true)
                setWhen(params.startedAt.toEpochMilli())

                val mainActivityIntent =
                    PendingIntentCompat.getActivity(
                        context,
                        MAIN_ACTIVITY_PENDING_INTENT_REQUEST_CODE,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        false,
                    )!!

                val stopScanningIntent =
                    PendingIntentCompat.getService(
                        context,
                        STOP_SCANNER_SERVICE_PENDING_INTENT_REQUEST_CODE,
                        params.stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        false,
                    )!!

                setContentIntent(mainActivityIntent)
                addAction(
                    NotificationCompat.Action(
                        R.drawable.stop_24px,
                        context.applicationContext.getTextCompat(R.string.stop),
                        stopScanningIntent,
                    )
                )
            }
            .build()
    }
}
