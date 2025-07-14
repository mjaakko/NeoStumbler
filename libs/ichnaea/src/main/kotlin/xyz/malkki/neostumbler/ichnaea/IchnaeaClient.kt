package xyz.malkki.neostumbler.ichnaea

import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import okio.BufferedSink
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateResponseDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto
import xyz.malkki.neostumbler.utils.io.closeShielded

private const val BUFFER_SIZE = 8 * 1024

private val HTTP_STATUS_CODE_SUCCESS = 200..299

/**
 * Client for communicating with an Ichnaea-compatible endpoint
 *
 * See
 * [https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html](https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html)
 */
class IchnaeaClient(
    private val httpClient: Call.Factory,
    private val ichnaeaParams: IchnaeaParams,
) : Geosubmit, Geolocate {
    companion object {
        private val JSON_ENCODER = Json {
            explicitNulls = false
            // BeaconDB geolocate responses include extra values not present in the Ichnaea format
            ignoreUnknownKeys = true
        }

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    override suspend fun sendReports(reports: List<ReportDto>) {
        val url =
            requireNotNull(ichnaeaParams.toSubmissionUrl()) {
                "Failed to create URL from params $ichnaeaParams"
            }

        val request =
            Request.Builder()
                .url(url)
                .post(createGeosubmitRequestBody(reports))
                .addHeader("Content-Encoding", "gzip")
                .build()

        val response = httpClient.newCall(request).executeAsync()
        response.use {
            if (response.code !in HTTP_STATUS_CODE_SUCCESS) {
                throw HttpException(request.url.toString(), response.code)
            }
        }
    }

    override suspend fun getLocation(requestDto: GeolocateRequestDto): GeolocateResponseDto {
        val url =
            requireNotNull(ichnaeaParams.toLocateUrl()) {
                "Failed to create URL from params: $ichnaeaParams"
            }

        val requestBody = JSON_ENCODER.encodeToString(requestDto).toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder().url(url).post(requestBody).build()

        httpClient.newCall(request).executeAsync().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(request.url.toString(), response.code)
            }

            return withContext(Dispatchers.IO) {
                JSON_ENCODER.decodeFromStream(response.body.byteStream().buffered())
            }
        }
    }

    private fun createGeosubmitRequestBody(reports: List<ReportDto>): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType = JSON_MEDIA_TYPE

            override fun writeTo(sink: BufferedSink) {
                GZIPOutputStream(sink.outputStream().closeShielded(), BUFFER_SIZE).buffered().use {
                    JSON_ENCODER.encodeToStream(ReportItems(reports), it)
                }
            }
        }
    }

    class HttpException(url: String, val httpStatusCode: Int) :
        IOException("HTTP request to $url failed, status: $httpStatusCode")

    @Serializable private data class ReportItems(val items: List<ReportDto>)
}
