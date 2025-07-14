package xyz.malkki.neostumbler.ichnaea

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
