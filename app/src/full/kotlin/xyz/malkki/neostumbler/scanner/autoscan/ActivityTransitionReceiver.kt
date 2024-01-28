package xyz.malkki.neostumbler.scanner.autoscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
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

        /**
         * @see ActivityTransitionReceiver.enableWithTask
         */
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        suspend fun enable(context: Context) {
            enableWithTask(context).await()
        }

        /**
         * Enables activity transition receiver for automatic scanning
         *
         * @return Task which completes when activity transition receiver is enabled
         */
        @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        fun enableWithTask(context: Context): Task<Void> {
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

            return activityRecognitionClient.requestActivityTransitionUpdates(activityTransitionRequest, pendingIntent)
        }

        suspend fun disable(context: Context) {
            val pendingIntent = getPendingIntent(context)

            //Removing activity transition updates without the permission causes a crash
            if (context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                val activityRecognitionClient = ActivityRecognition.getClient(context)

                activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent).await()
            }

            //Cancel the pending intent to make sure that we don't receive more activity transition updates
            pendingIntent.cancel()
        }
    }

    @SuppressLint("MissingPermission")
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

                LocationReceiver.requestLocationUpdateToStartAutoscan(context)
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