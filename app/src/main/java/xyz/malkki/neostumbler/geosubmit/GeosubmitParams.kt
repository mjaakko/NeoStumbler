package xyz.malkki.neostumbler.geosubmit

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class GeosubmitParams(val baseUrl: String, val path: String, val apiKey: String?) {
    companion object {
        const val DEFAULT_PATH = "/v2/geosubmit"
    }

    fun toUrl(): HttpUrl? {
        val urlStr = "$baseUrl$path"
        val url = urlStr.removeConsecutiveSlashes().toHttpUrlOrNull()

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
