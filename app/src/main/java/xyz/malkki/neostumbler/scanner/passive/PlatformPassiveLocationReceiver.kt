package xyz.malkki.neostumbler.scanner.passive

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.coroutinebroadcastreceiver.CoroutineBroadcastReceiver
import xyz.malkki.neostumbler.mapper.toPositionObservation

class PlatformPassiveLocationReceiver : CoroutineBroadcastReceiver(), KoinComponent {
    companion object {
        private const val REQUEST_CODE = 123895

        fun getPendingIntent(context: Context): PendingIntent {
            val intent =
                Intent(context.applicationContext, PlatformPassiveLocationReceiver::class.java)

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

    private fun Intent.getLocations(): List<Location> {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                hasExtra(LocationManager.KEY_LOCATIONS)
        ) {
            IntentCompat.getParcelableArrayExtra(
                    this,
                    LocationManager.KEY_LOCATIONS,
                    Location::class.java,
                )
                ?.filterIsInstance<Location>() ?: emptyList()
        } else if (hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
            listOfNotNull(
                IntentCompat.getParcelableExtra(
                    this,
                    LocationManager.KEY_LOCATION_CHANGED,
                    Location::class.java,
                )
            )
        } else {
            emptyList()
        }
    }

    override suspend fun handleIntent(context: Context, intent: Intent) {
        val locations: List<Location> = intent.getLocations()

        val positions =
            locations.map { location ->
                val source =
                    when (location.provider) {
                        LocationManager.GPS_PROVIDER -> {
                            Position.Source.GPS
                        }
                        LocationManager.NETWORK_PROVIDER -> {
                            Position.Source.NETWORK
                        }
                        else -> {
                            // Location source unknown, let's use FUSED as the best guess
                            Position.Source.FUSED
                        }
                    }

                location.toPositionObservation(source = source)
            }

        @SuppressLint("MissingPermission")
        passiveScanReportCreator.createPassiveScanReport(positions)
    }
}
