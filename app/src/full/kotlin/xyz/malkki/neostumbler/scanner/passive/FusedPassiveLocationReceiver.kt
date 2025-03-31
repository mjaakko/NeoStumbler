package xyz.malkki.neostumbler.scanner.passive

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.malkki.neostumbler.domain.Position

class FusedPassiveLocationReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private const val REQUEST_CODE = 123894

        fun getPendingIntent(context: Context): PendingIntent {
            val intent =
                Intent(context.applicationContext, FusedPassiveLocationReceiver::class.java)

            return PendingIntentCompat.getBroadcast(
                context.applicationContext,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                true,
            )!!
        }
    }

    private val passiveScanReportCreator: PassiveScanReportCreator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (LocationResult.hasResult(intent)) {
            val locationResult = LocationResult.extractResult(intent)!!

            val positions =
                locationResult.locations.map {
                    Position.fromLocation(it, source = Position.Source.FUSED)
                }

            runBlocking {
                // We can assume that we have the location permission, because we are receiving
                // locations
                @SuppressLint("MissingPermission")
                passiveScanReportCreator.createPassiveScanReport(positions)
            }
        }
    }
}
