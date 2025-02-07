package xyz.malkki.neostumbler.geosubmit

import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.apache.commons.io.output.CloseShieldOutputStream
import xyz.malkki.neostumbler.extensions.executeSuspending
import xyz.malkki.neostumbler.geosubmit.dto.ReportDto

private const val BUFFER_SIZE = 8 * 1024

/**
 * Geosubmit implementation for sending data to an Ichnaea endpoint
 *
 * See
 * [https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html)
 */
class IchnaeaGeosubmit(
    private val httpClient: Call.Factory,
    private val geosubmitParams: GeosubmitParams,
) : Geosubmit {
    companion object {
        private val JSON_ENCODER = Json { explicitNulls = false }
    }

    override suspend fun sendReports(reports: List<ReportDto>) {
        val url = geosubmitParams.toUrl()
        require(url != null) { "Failed to create URL from params $geosubmitParams" }

        val request =
            Request.Builder()
                .url(url)
                .post(createRequestBody(reports))
                .addHeader("Content-Encoding", "gzip")
                .build()

        val response = httpClient.newCall(request).executeSuspending()
        response.use {
            if (response.code !in 200..299) {
                throw IchnaeaGeosubmitException(
                    "HTTP request to ${request.url} failed, status: ${response.code}",
                    response.code,
                )
            }
        }
    }

    private fun createRequestBody(reports: List<ReportDto>): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType = "application/json".toMediaType()

            override fun writeTo(sink: BufferedSink) {
                GZIPOutputStream(CloseShieldOutputStream.wrap(sink.outputStream()), BUFFER_SIZE)
                    .buffered()
                    .use { JSON_ENCODER.encodeToStream(ReportItems(reports), it) }
            }
        }
    }

    class IchnaeaGeosubmitException(message: String, val httpStatusCode: Int) :
        IOException(message)

    @Serializable private data class ReportItems(val items: List<ReportDto>)
}
