package xyz.malkki.neostumbler.geosubmit

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


data class GeosubmitParams(
    val baseUrl: String,
    val path: String,
    val apiKey: String?
) {
    companion object {
        //NOTE: MLS does not accept data submissions anymore, this is used as a default just to show an example
        const val DEFAULT_BASE_URL = "https://location.services.mozilla.com"

        const val DEFAULT_PATH = "/v2/geosubmit"
    }

    fun toUrl(): HttpUrl? {
        val url = "$baseUrl$path".toHttpUrlOrNull()

        return if (apiKey != null && url != null) {
            url.newBuilder()
                .addQueryParameter("key", apiKey)
                .build()
        } else {
            url
        }
    }
}
