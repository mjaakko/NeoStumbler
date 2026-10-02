package xyz.malkki.neostumbler.utils

import android.os.Build
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import xyz.malkki.neostumbler.BuildConfig

private fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

private const val URL_MAX_LENGTH = 2000

private const val BUG_REPORT_BASE_URL =
    "https://github.com/mjaakko/NeoStumbler/issues/new?labels=bug&template=1-bug_report.yml"

fun getBugReportUrl(logs: String? = null): String {
    return buildString {
        val manufacturer =
            Build.BRAND.takeIf { !it.isNullOrBlank() }?.replaceFirstChar { it.uppercaseChar() }
                ?: Build.MANUFACTURER

        val device = "$manufacturer ${Build.MODEL}"

        append(BUG_REPORT_BASE_URL)
        append("&version=${BuildConfig.VERSION_NAME.urlEncode()}")
        append("&variant=${BuildConfig.FLAVOR.urlEncode()}")
        append("&android-version=${Build.VERSION.RELEASE.urlEncode()}")
        append("&device=${device.urlEncode()}")

        logs?.urlEncode()?.let {
            append("&logs=")

            val remainingSpace = (URL_MAX_LENGTH - length).coerceAtLeast(0)

            append(it.take(remainingSpace))
        }
    }
}
