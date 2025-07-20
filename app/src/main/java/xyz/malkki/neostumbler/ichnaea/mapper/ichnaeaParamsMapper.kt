package xyz.malkki.neostumbler.ichnaea.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.ichnaea.IchnaeaParams

fun Settings.getIchnaeaParamsFlow(): Flow<IchnaeaParams?> =
    getSnapshotFlow().map { prefs ->
        val baseUrl = prefs.getString(PreferenceKeys.GEOSUBMIT_ENDPOINT)
        val submissionPath =
            prefs.getString(PreferenceKeys.GEOSUBMIT_PATH) ?: IchnaeaParams.DEFAULT_SUBMISSION_PATH
        val locatePath =
            prefs.getString(PreferenceKeys.GEOLOCATE_PATH) ?: IchnaeaParams.DEFAULT_LOCATE_PATH
        val apiKey = prefs.getString(PreferenceKeys.GEOSUBMIT_API_KEY)

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

suspend fun Settings.getIchnaeaParams(): IchnaeaParams? = getIchnaeaParamsFlow().first()
