package xyz.malkki.neostumbler.ichnaea.mapper

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ichnaea.IchnaeaParams

suspend fun DataStore<Preferences>.getIchnaeaParams(): IchnaeaParams? =
    data
        .map { prefs ->
            val baseUrl = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)]
            val submissionPath =
                prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)]
                    ?: IchnaeaParams.DEFAULT_SUBMISSION_PATH
            val locatePath =
                prefs[stringPreferencesKey(PreferenceKeys.GEOLOCATE_PATH)]
                    ?: IchnaeaParams.DEFAULT_LOCATE_PATH
            val apiKey = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

            if (baseUrl != null) {
                IchnaeaParams(
                    baseUrl = baseUrl,
                    submissionPath = submissionPath,
                    locatePath = locatePath,
                    apiKey = apiKey,
                )
            } else {
                null
            }
        }
        .first()
