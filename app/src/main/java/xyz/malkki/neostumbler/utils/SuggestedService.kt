package xyz.malkki.neostumbler.utils

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import xyz.malkki.neostumbler.R

@Serializable
data class SuggestedService(
    /**
     * Used for uniquely identifying the specific service
     */
    val id: String,
    val name: String,
    val description: String? = null,
    val website: String,
    val termsOfUse: String,
    val hostedBy: String,
    val endpoint: Endpoint,
    val coverageTileJsonUrl: String
) {
    companion object {
        fun getSuggestedServices(context: Context): List<SuggestedService> {
            return context.resources.openRawResource(R.raw.suggested_services)
                .buffered()
                .use {
                    Json.decodeFromStream<List<SuggestedService>>(it).shuffled()
                }
        }
    }

    @Serializable
    data class Endpoint(
        val baseUrl: String,
        val path: String,
        val apiKey: String? = null
    )
}