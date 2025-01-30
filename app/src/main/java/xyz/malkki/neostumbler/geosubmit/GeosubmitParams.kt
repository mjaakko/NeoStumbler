package xyz.malkki.neostumbler.geosubmit

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys


data class GeosubmitParams(
    val baseUrl: String,
    val path: String,
    val apiKey: String?
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.beacondb.net"
        const val MLS_BASE_URL = "https://location.services.mozilla.com"

        const val DEFAULT_PATH = "/v2/geosubmit"
    }

    fun toUrl(): HttpUrl? {
        val urlStr = "$baseUrl$path"
        val url = urlStr.removeConsecutiveSlashes().toHttpUrlOrNull()

        return if (apiKey != null && url != null) {
            url.newBuilder()
                .addQueryParameter("key", apiKey)
                .build()
        } else {
            url
        }
    }

    /**
     * Removes consecutive slashes from the URL, as it's probably just a typo
     */
    private fun String.removeConsecutiveSlashes(): String {
        return mapIndexedNotNull { i, char ->
            val next = getOrNull(i + 1)

            if (char == '/' && char == next) {
                null
            } else {
                char
            }
        }.joinToString("")
    }
}

fun StumblerApplication.geosubmitParamsFlow(): Flow<GeosubmitParams> {
    return this.settingsStore.data
        .map { prefs ->
            val endpoint = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)]
                ?: GeosubmitParams.DEFAULT_BASE_URL
            val path = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)]
                ?: GeosubmitParams.DEFAULT_PATH
            val apiKey = prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

            GeosubmitParams(endpoint, path, apiKey)
        }
}
