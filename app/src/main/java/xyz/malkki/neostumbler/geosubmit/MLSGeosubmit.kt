package xyz.malkki.neostumbler.geosubmit

import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import xyz.malkki.neostumbler.extensions.executeSuspending
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

private const val BUFFER_SIZE = 8 * 1024

class MLSGeosubmit(private val httpClient: OkHttpClient, private val gson: Gson, private val baseUrl: String = "https://location.services.mozilla.com") : Geosubmit {
    override suspend fun sendReports(reports: List<Report>) {
        val request = Request.Builder().url("$baseUrl/v2/geosubmit").post(createRequestBody(reports)).addHeader("Content-Encoding", "gzip").build()

        val response = httpClient.newCall(request).executeSuspending()
        response.use {
            if (response.code !in 200..299) {
                throw MLSException("HTTP request to ${request.url} failed, status: ${response.code}", response.code)
            }
        }
    }

    private fun createRequestBody(reports: List<Report>): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType = "application/json".toMediaType()

            override fun writeTo(sink: BufferedSink) {
                OutputStreamWriter(GZIPOutputStream(sink.outputStream(), BUFFER_SIZE), StandardCharsets.UTF_8).use {
                    gson.toJson(mapOf("items" to reports), it)
                }
            }
        }
    }

    class MLSException(message: String, val httpStatusCode: Int) : IOException(message)
}