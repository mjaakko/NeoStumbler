package xyz.malkki.neostumbler.scanner.autoscan

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.scanner.autoscan.LocationReceiver.Companion.AUTOSCAN_GEOFENCE_REQUEST_ID
import java.time.Duration

class ActivityTransitionReceiver : BroadcastReceiver() {
    companion object {
        private const val PENDING_INTENT_REQUEST_CODE = 1111

        private fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                PENDING_INTENT_REQUEST_CODE,
                Intent(context, ActivityTransitionReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        @SuppressLint("MissingPermission")
        suspend fun enable(context: Context) {
            val activityRecognitionClient = ActivityRecognition.getClient(context)

            val activityTypes = listOf(DetectedActivity.STILL)
            val transitionTypes = listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT)

            val activityTransitionRequest = ActivityTransitionRequest(
                activityTypes.flatMap { activityType ->
                    transitionTypes.map { transitionType ->
                        ActivityTransition.Builder()
                            .setActivityType(activityType)
                            .setActivityTransition(transitionType)
                            .build()
                    }
                }
            )
            val pendingIntent = getPendingIntent(context)

            activityRecognitionClient.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent).await()
        }

        @SuppressLint("MissingPermission")
        suspend fun disable(context: Context) {
            val activityRecognitionClient = ActivityRecognition.getClient(context)

            activityRecognitionClient.removeActivityTransitionUpdates(getPendingIntent(context)).await()
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryStartAutoscan(context: Context) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        //Request location to check whether user is in a location where reports have not been made
        val locationRequest = LocationRequest.Builder(0)
            .setDurationMillis(10 * 60 * 1000)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(30 * 1000)
            .setMaxUpdates(1)
            .build()

        Timber.i("Requesting location update to determine if scanning should be started")
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            LocationReceiver.getPendingIntent(context)
        )
    }

    private fun handleActivityTransitionResult(context: Context, activityTransitionResult: ActivityTransitionResult) {
        for (event in activityTransitionResult.transitionEvents) {
            val age = Duration.ofNanos(SystemClock.elapsedRealtimeNanos() - event.elapsedRealTimeNanos)

            if (event.activityType == DetectedActivity.STILL && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                Timber.i("User entered STILL activity ${age.seconds}s ago")

                //Stop scanning
                context.startService(ScannerService.stopIntent(context, autostart = true))

                //Also remove geofences here to make sure that geofencing events don't start the scanning service
                Timber.i("Removing pending location updates")
                val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationProviderClient.removeLocationUpdates(LocationReceiver.getPendingIntent(context))

                Timber.i("Removing geofences")
                val geofencingClient = LocationServices.getGeofencingClient(context)
                geofencingClient.removeGeofences(listOf(AUTOSCAN_GEOFENCE_REQUEST_ID))
            } else if (event.activityType == DetectedActivity.STILL && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                Timber.i("User exited STILL activity ${age.seconds}s ago")

                tryStartAutoscan(context)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!

            handleActivityTransitionResult(context.applicationContext, result)
        }
    }
}