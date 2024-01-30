package xyz.malkki.neostumbler.extensions

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Checks if Google APIs are available
 *
 * @return true is Google APIs are available, false if not
 */
fun Context.isGoogleApisAvailable(): Boolean {
    return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext) == ConnectionResult.SUCCESS
}