package xyz.malkki.neostumbler.utils

import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber

fun getTileJsonLayerIds(tileJsonUrl: String?, httpClient: Call.Factory, callback: (List<String>) -> Unit) {
    val layerIds = mutableListOf<String>()
    if (tileJsonUrl != null) {
        httpClient.newCall(Request.Builder().url(tileJsonUrl).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                Timber.e(error, "TileJSON request failed")
                callback(layerIds)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { jsonString ->
                            runCatching {
                                JSONObject(jsonString).optJSONArray("vector_layers")
                            }.onSuccess { vectorLayers ->
                                vectorLayers?.let {
                                    for (i in 0 until it.length()) {
                                        layerIds.add(it.getJSONObject(i).getString("id"))
                                    }
                                }
                            }.onFailure { error ->
                                Timber.e(error, "TileJSON parser failed")
                            }
                        }
                    }
                }
                callback(layerIds)
            }
        })
    } else {
        callback(layerIds)
    }
}
