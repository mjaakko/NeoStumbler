package xyz.malkki.neostumbler.ichnaeaupload

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.ichnaea.IchnaeaClient
import xyz.malkki.neostumbler.ichnaea.IchnaeaParams
import xyz.malkki.neostumbler.network.HttpCallFactoryProvider

class IchnaeaClientProvider(
    private val httpClientProvider: HttpCallFactoryProvider,
    private val settings: Settings,
) {
    val ichnaeaParams: Flow<IchnaeaParams?> =
        settings.getSnapshotFlow().map { settingsSnapshot ->
            val baseUrl = settingsSnapshot.getString(IchnaeaPreferenceKeys.GEOSUBMIT_ENDPOINT)
            val submissionPath =
                settingsSnapshot.getString(IchnaeaPreferenceKeys.GEOSUBMIT_PATH)
                    ?: IchnaeaParams.DEFAULT_SUBMISSION_PATH
            val locatePath =
                settingsSnapshot.getString(IchnaeaPreferenceKeys.GEOLOCATE_PATH)
                    ?: IchnaeaParams.DEFAULT_LOCATE_PATH
            val apiKey = settingsSnapshot.getString(IchnaeaPreferenceKeys.GEOSUBMIT_API_KEY)

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

    val ichnaeaClient: Flow<IchnaeaClient?> =
        ichnaeaParams.map { params ->
            params?.let { IchnaeaClient(httpClientProvider.getHttpCallFactory(), it) }
        }

    suspend fun setIchnaeaParams(ichnaeaParams: IchnaeaParams) {
        settings.edit {
            setString(IchnaeaPreferenceKeys.GEOSUBMIT_ENDPOINT, ichnaeaParams.baseUrl)
            setString(IchnaeaPreferenceKeys.GEOLOCATE_PATH, ichnaeaParams.submissionPath)
            if (ichnaeaParams.locatePath != null) {
                setString(IchnaeaPreferenceKeys.GEOLOCATE_PATH, ichnaeaParams.locatePath!!)
            } else {
                removeString(IchnaeaPreferenceKeys.GEOLOCATE_PATH)
            }
            if (ichnaeaParams.apiKey != null) {
                setString(IchnaeaPreferenceKeys.GEOSUBMIT_API_KEY, ichnaeaParams.apiKey!!)
            } else {
                removeString(IchnaeaPreferenceKeys.GEOSUBMIT_API_KEY)
            }
        }
    }
}
