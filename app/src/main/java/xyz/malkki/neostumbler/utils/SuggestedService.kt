package xyz.malkki.neostumbler.utils

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import xyz.malkki.neostumbler.R
import java.nio.charset.StandardCharsets

@Keep
data class SuggestedService(
    val name: String,
    val description: String?,
    val website: String,
    val termsOfUse: String,
    val hostedBy: String,
    val endpoint: Endpoint
) {
    companion object {
        fun getSuggestedServices(context: Context): List<SuggestedService> {
            return context.resources.openRawResource(R.raw.suggested_services)
                .bufferedReader(StandardCharsets.UTF_8)
                .use {
                    Gson().fromJson(it, Array<SuggestedService>::class.java)
                        .toList()
                        .shuffled()
                }
        }
    }

    data class Endpoint(
        val baseUrl: String,
        val path: String,
        val apiKey: String?
    )
}