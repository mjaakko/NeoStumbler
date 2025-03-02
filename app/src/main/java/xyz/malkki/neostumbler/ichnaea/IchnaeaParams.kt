package xyz.malkki.neostumbler.ichnaea

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import xyz.malkki.neostumbler.constants.PreferenceKeys

data class IchnaeaParams(
    val baseUrl: String,
    val submissionPath: String,
    val locatePath: String?,
    val apiKey: String?,
) {
    companion object {
        const val DEFAULT_SUBMISSION_PATH = "/v2/geosubmit"
        const val DEFAULT_LOCATE_PATH = "/v1/geolocate"
    }

    fun toSubmissionUrl(): HttpUrl? {
        val urlStr = "$baseUrl$submissionPath"

        return buildUrl(urlStr)
    }

    fun toLocateUrl(): HttpUrl? {
        val urlStr = "$baseUrl$locatePath"

        return buildUrl(urlStr)
    }

    private fun buildUrl(urlString: String): HttpUrl? {
        val url = urlString.removeConsecutiveSlashes().toHttpUrlOrNull()

        return if (apiKey != null && url != null) {
            url.newBuilder().addQueryParameter("key", apiKey).build()
        } else {
            url
        }
    }

    /** Removes consecutive slashes from the URL, as it's probably just a typo */
    private fun String.removeConsecutiveSlashes(): String {
        return mapIndexedNotNull { i, char ->
                val next = getOrNull(i + 1)

                if (char == '/' && char == next) {
                    null
                } else {
                    char
                }
            }
            .joinToString("")
    }
}

suspend fun DataStore<Preferences>.getIchnaeaParams(): IchnaeaParams? =
    data
        .map { prefs ->
            val baseUrl = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)]
            val submissionPath =
                prefs[stringPreferencesKey(PreferenceKeys.GEOLOCATE_PATH)]
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
