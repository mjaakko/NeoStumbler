package xyz.malkki.neostumbler.scanner.autoscan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.scanner.ScannerService
import java.time.Duration
import java.time.Instant


class LocationReceiver : BroadcastReceiver() {
    companion object {
        const val AUTOSCAN_GEOFENCE_REQUEST_ID = "autoscan"

        //Check whether reports have been created within this distance from the current location
        private const val REPORT_RADIUS = 300.0

        private const val PENDING_INTENT_REQUEST_CODE = 4444

        //If more than this amount of reports have been created within the radius of of current location, do not autostart scanning
        private const val MAX_REPORTS_AUTOSTART = 5

        fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                PENDING_INTENT_REQUEST_CODE,
                Intent(context, LocationReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (LocationAvailability.hasLocationAvailability(intent)) {
            val isLocationAvailable = LocationAvailability.extractLocationAvailability(intent)?.isLocationAvailable

            Timber.i("Received location availability event, is location available: $isLocationAvailable")
        }

        if (LocationResult.hasResult(intent)) {
            handleLocation(context.applicationContext, LocationResult.extractResult(intent)!!)
        } else {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                handleGeofencingEvent(context.applicationContext, geofencingEvent)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryStartAutoscan(context: Context, currentLocation: Location) {
        val reportDao = (context.applicationContext as StumblerApplication).reportDb.reportDao()

        val reportMinTimestamp = Instant.now().minus(Duration.ofDays(30))
        Timber.d("Querying reports newer than $reportMinTimestamp")
        val reports = runBlocking { reportDao.getReportsNewerThan(reportMinTimestamp) }

        val reportsNearCurrentLocation = reports.count { report ->
            Location("manual").apply {
                latitude = report.latitude
                longitude = report.longitude
            }.distanceTo(currentLocation) <= REPORT_RADIUS
        }

        val canStartScanningHere = reportsNearCurrentLocation < MAX_REPORTS_AUTOSTART

        Timber.i("Current location is ${currentLocation.latitude}, ${currentLocation.longitude}. $reportsNearCurrentLocation reports have been made near (< ${REPORT_RADIUS}m) the current location within last month. Start autoscanning: $canStartScanningHere")

        //NOTE: Can't use coroutine API .await() with geofences here because it causes geofence requests to hang for some reason
        val geofencingClient = LocationServices.getGeofencingClient(context)

        //If less than 5 reports have been created near the current location in the last month, start scanning
        if (canStartScanningHere) {
            context.startForegroundService(ScannerService.startIntent(context, autostart = true))

            //Remove geofences because scanning was started and we don't need them anymore
            geofencingClient.removeGeofences(listOf(AUTOSCAN_GEOFENCE_REQUEST_ID))
                .addOnSuccessListener { Timber.i("Removed geofences") }
                .addOnFailureListener { e -> Timber.w(e, "Failed to remove geofences") }
        } else {
            //Otherwise create geofence to detect when user has moved away from the current location and try again
            val geofencingRequest = GeofencingRequest.Builder()
                .addGeofence(Geofence.Builder()
                    .setRequestId(AUTOSCAN_GEOFENCE_REQUEST_ID)
                    //Use slightly larger radius for geofence to make it more likely that scanning is possible when user exits geofence
                    .setCircularRegion(currentLocation.latitude, currentLocation.longitude, REPORT_RADIUS.toFloat() * 1.2f)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setNotificationResponsiveness(60 * 1000)
                    .setExpirationDuration(30 * 60 * 1000)
                    .build()
                )
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .build()

            Timber.i("Adding geofence to detect when user moves away from the current location")

            geofencingClient.addGeofences(geofencingRequest, getPendingIntent(context))
                .addOnSuccessListener { Timber.i("Added geofences") }
                .addOnFailureListener { e -> Timber.w(e, "Failed to add geofences") }
        }
    }

    private fun handleGeofencingEvent(context: Context, geofencingEvent: GeofencingEvent) {
        if (geofencingEvent.hasError()) {
            Timber.w("Geofencing event had an error: ${GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)} (${geofencingEvent.errorCode})")

            //TODO: disable autoscanning and prompt user to re-enable
            //https://developer.android.com/training/location/geofencing#re-register-geofences-only-when-required
        } else {
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT && geofencingEvent.triggeringGeofences?.any { it.requestId == AUTOSCAN_GEOFENCE_REQUEST_ID } == true) {
                Timber.i("Received geofence exit event")

                val currentLocation = geofencingEvent.triggeringLocation!!

                tryStartAutoscan(context, currentLocation)
            } else {
                Timber.w("Received unexpected geofence event (transition: ${geofencingEvent.geofenceTransition}, request IDs: ${geofencingEvent.triggeringGeofences?.map { it.requestId }})")
            }
        }
    }

    private fun handleLocation(context: Context, locationResult: LocationResult) {
        if (locationResult.lastLocation != null) {
            val location = locationResult.lastLocation!!

            Timber.i("Received new location event (${location.latitude}, ${location.longitude})")
            tryStartAutoscan(context, location)
        } else {
            Timber.i("Received new location event, but location was null")
        }
    }
}