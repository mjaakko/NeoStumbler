package xyz.malkki.neostumbler.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

suspend fun getTileJsonLayerIds(tileJsonUrl: String, httpClient: Call.Factory): List<String> {
    val url = tileJsonUrl.toHttpUrlOrNull() ?: return emptyList()

    val response = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

    return withContext(Dispatchers.IO) {
        response.use {
            if (!it.isSuccessful) {
                Timber.w("TileJSON request failed (HTTP status: %d)", it.code)

                emptyList()
            } else {
                it.body.string()?.let { jsonString ->
                    try {
                        val vectorLayers = JSONObject(jsonString).optJSONArray("vector_layers")

                        buildList<String> {
                            vectorLayers?.let {
                                for (i in 0 until it.length()) {
                                    add(it.getJSONObject(i).getString("id"))
                                }
                            }
                        }
                    } catch (ex: JSONException) {
                        Timber.e(ex, "TileJSON parser failed")

                        emptyList<String>()
                    }
                } ?: emptyList<String>()
            }
        }
    }
}
